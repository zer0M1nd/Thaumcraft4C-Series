package tc4revival.content;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Data-pack candidates feed vanilla level selection, XP, demand, discounts and workstation restock. */
public class VillagerTradeData {
    private static volatile Map<ResourceLocation,VillagerTradeDefinition> definitions=Map.of();
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void trades(VillagerTradesEvent event) {
        var definition=definitions.get(ForgeRegistries.VILLAGER_PROFESSIONS.getKey(event.getType()));
        if(definition==null)return;
        definition.levels().forEach((level,rows)->{
            var list=event.getTrades().get(level);
            list.removeIf(x->x instanceof Listing);
            rows.forEach(row->list.add(new Listing(row)));
        });
    }
    public static Map<ResourceLocation,VillagerTradeDefinition> definitions() { return definitions; }
    public static class Listing implements VillagerTrades.ItemListing {
        private final VillagerTradeDefinition.Trade row;
        public Listing(VillagerTradeDefinition.Trade row) { this.row=row; }
        private static ItemStack stack(VillagerTradeDefinition.Stack spec,RandomSource random) {
            if(spec==null)return ItemStack.EMPTY;
            var id=ResourceLocation.parse(spec.items().get(random.nextInt(spec.items().size())));
            var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id),spec.min()+random.nextInt(spec.max()-spec.min()+1));
            if(spec.nbt()!=null)try { stack.setTag(net.minecraft.nbt.TagParser.parseTag(spec.nbt())); }
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException("Invalid trade NBT",e);}
            return stack;
        }
        @Override public MerchantOffer getOffer(Entity merchant,RandomSource random) {
            var offer=new MerchantOffer(stack(row.buy(),random),stack(row.buyB(),random),stack(row.sell(),random),row.maxUses(),row.xp(),row.multiplier());
            if(!row.rewardExp()){var tag=offer.createTag();tag.putBoolean("rewardExp",false);offer=new MerchantOffer(tag);}
            return offer;
        }
    }
    private static void validate(VillagerTradeDefinition.Stack stack) {
        if(stack==null)return;
        for(var id:stack.items())if(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id))||id.equals("minecraft:air"))throw new IllegalArgumentException("Unknown trade item: "+id);
        if(stack.nbt()!=null)try { net.minecraft.nbt.TagParser.parseTag(stack.nbt()); }
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException("Invalid trade NBT",e);}
    }
    public static class Reload extends SimpleJsonResourceReloadListener {
        public Reload() { super(new Gson(),"villager_trades"); }
        @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,ProfilerFiller profiler) {
            var loaded=new LinkedHashMap<ResourceLocation,VillagerTradeDefinition>();
            for(String name:List.of("wizard","banker")) {
                var id=ResourceLocation.parse("thaumcraft:"+name);
                if(!files.containsKey(id))throw new IllegalArgumentException("Missing villager trades: "+id);
                try {
                    var def=VillagerTradeDefinition.decode(files.get(id).getAsJsonObject());
                    def.levels().values().forEach(rows->rows.forEach(row->{validate(row.buy());validate(row.buyB());validate(row.sell());}));
                    loaded.put(id,def);
                }catch(RuntimeException e){throw new IllegalArgumentException("Invalid villager trades "+id,e);}
            }
            // Parse and validate both professions before changing the live tables. Preserve
            // other mods' Forge-event additions when replacing our own entries on /reload.
            loaded.forEach((id,def)->{
                var profession=ForgeRegistries.VILLAGER_PROFESSIONS.getValue(id);
                var table=VillagerTrades.TRADES.computeIfAbsent(profession,p->new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>());
                def.levels().forEach((level,rows)->{
                    var entries=new ArrayList<VillagerTrades.ItemListing>();
                    if(table.get(level)!=null)for(var entry:table.get(level))if(!(entry instanceof Listing))entries.add(entry);
                    rows.forEach(row->entries.add(new Listing(row)));
                    table.put(level,entries.toArray(VillagerTrades.ItemListing[]::new));
                });
            });
            definitions=Map.copyOf(loaded);
        }
    }
}
