package tc4revival.debug;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.*;

/** Read-only snapshot of loaded registry/data values, with isolated client-evaluator parity checks. */
public class AspectTableExportCommand {
    public static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack,?> tree(){
        return Commands.literal("export_aspect_table").then(Commands.argument("name",StringArgumentType.word()).executes(c->{try{return export(c.getSource(),StringArgumentType.getString(c,"name"));}catch(Exception e){tc4revival.TC4Revival.LOGGER.error("Aspect export failed",e);c.getSource().sendFailure(Component.literal("Aspect export failed: "+e.getMessage()));return 0;}}));
    }
    public static String complex(ItemStack stack){
        var item=stack.getItem();
        if(item instanceof PotionItem || item==Items.TIPPED_ARROW)return "potion";
        if(item instanceof SpawnEggItem)return "entity_variant";
        if(item instanceof tc4revival.content.CastingWandItem)return "wand_parts";
        if(item instanceof tc4revival.content.StoredAspectItem)return "stored_aspects";
        if(item instanceof tc4revival.content.ItemMirrorItem || item instanceof tc4revival.content.EssentiaMirrorItem || item instanceof tc4revival.content.SinisterStoneItem)return "linked_metadata";
        if(item==Items.FILLED_MAP || item==Items.ENCHANTED_BOOK || item==Items.FIREWORK_ROCKET || item==Items.FIREWORK_STAR)return "nbt_variant";
        return null;
    }
    private static JsonObject values(AspectAmounts amounts){
        var row=new JsonObject();if(amounts!=null)amounts.view().forEach((a,n)->row.addProperty(a.tag(),n));return row;
    }
    public static int export(CommandSourceStack source,String name)throws java.io.IOException{
        if(!name.matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException("Use a simple export name");
        var dir=source.getServer().getWorldPath(LevelResource.ROOT).resolve("aspect-exports").resolve(name);
        if(Files.exists(dir))throw new IllegalArgumentException("Export exists: "+dir);
        Files.createDirectories(dir);
        var root=new JsonObject();root.addProperty("format",1);root.addProperty("created",java.time.Instant.now().toString());
        var snapshot=tc4revival.aspect.sync.ServerAspectData.snapshot();
        var frames=tc4revival.aspect.sync.AspectDataTransfer.encode(snapshot.files());
        var receiver=new tc4revival.aspect.sync.AspectDataTransfer.Receiver();Map<String,String> received=null;
        for(int i=0;i<frames.size();i++)received=receiver.accept(i,frames.size(),frames.get(i)).orElse(received);
        if(!snapshot.files().equals(received))throw new IllegalStateException("Aspect snapshot transfer mismatch");
        var local=new tc4revival.aspect.sync.AspectCalculationData(new tc4revival.aspect.sync.AspectDataSet(received),tc4revival.aspect.TCAspects.catalog(),
                (id,tag)->ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id)).builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation(tag))));
        var mods=new JsonObject();ModList.get().getMods().forEach(m->mods.addProperty(m.getModId(),m.getVersion().toString()));root.add("mods",mods);
        var items=new JsonArray();var itemCsv=new StringBuilder("id,status,aspects\n");
        for(var id:ForgeRegistries.ITEMS.getKeys().stream().sorted().toList()){
            var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id));var row=new JsonObject();row.addProperty("id",id.toString());
            var reason=complex(stack);row.addProperty("complex",reason!=null);if(reason!=null)row.addProperty("excluded_reason",reason);
            row.addProperty("class",stack.getItem().getClass().getName());
            row.addProperty("direct_rule_count",ItemAspects.directResolver().rules(id.toString()).size());
            row.addProperty("nbt_rule_count",ItemAspects.directResolver().rules(id.toString()).stream().filter(r->!r.nbt().isEmpty()).count());
            var resolved=stack.isEmpty()?Optional.<AspectAmounts>empty():ItemStackAspects.resolve(stack);
            var client=stack.isEmpty()?Optional.<AspectAmounts>empty():ItemStackAspects.resolve(stack,local);
            if(!values(resolved.orElse(null)).equals(values(client.orElse(null))))throw new IllegalStateException("Client/server aspect mismatch: "+id);
            row.add("aspects",values(resolved.orElse(null)));row.addProperty("defined",resolved.isPresent());
            row.add("base",values(ItemAspects.baseSession().base(id.toString()).orElse(null)));
            items.add(row);itemCsv.append(id).append(',').append(reason==null?"simple":reason).append(',').append(csv(row.get("aspects").toString())).append('\n');
        }
        root.add("items",items);
        var variants=new JsonArray();var variantCsv=new StringBuilder("id,selector,witness_snbt,aspects\n");
        var serverCache=new ItemAspectCache(ItemAspectAccess.SERVER);var clientCache=new ItemAspectCache(local);
        for(var id:ForgeRegistries.ITEMS.getKeys().stream().sorted().toList()) {
            var predicates=new LinkedHashSet<ItemNbtPredicate>();
            ItemAspects.directResolver().rules(id.toString()).forEach(r->{if(!r.nbt().isEmpty())predicates.add(r.nbt());});
            ItemStackAspects.equipmentResolver().rules(id.toString()).forEach(r->{if(!r.nbt().isEmpty())predicates.add(r.nbt());});
            for(var predicate:predicates) {
                var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id));stack.setTag(predicate.example());
                var actual=ItemStackAspects.resolve(stack).orElse(null);var client=ItemStackAspects.resolve(stack,local).orElse(null);
                if(!values(actual).equals(values(client)))throw new IllegalStateException("Client/server NBT aspect mismatch: "+id+" "+predicate.canonical());
                if(!values(actual).equals(values(serverCache.resolve(stack).amounts())) || !values(client).equals(values(clientCache.resolve(stack).amounts())))
                    throw new IllegalStateException("NBT cache mismatch: "+id+" "+predicate.canonical());
                var key=ItemAspectCache.key(stack,local);var identity=ItemStackAspects.scanHash(stack,local);
                var renamed=stack.copy();renamed.getOrCreateTag().putString("omniverse_export_unrelated","ignored");
                boolean callback=stack.getItem() instanceof tc4revival.content.StoredAspectItem || stack.getItem() instanceof tc4revival.content.CastingWandItem && stack.getItem().getClass()!=tc4revival.content.CastingWandItem.class;
                if(!callback&&!key.equals(ItemAspectCache.key(renamed,local)) || !identity.equals(ItemStackAspects.scanHash(renamed,local)))
                    throw new IllegalStateException("Unrelated NBT changed selector identity: "+id);
                var row=new JsonObject();row.addProperty("id",id.toString());row.add("nbt",predicate.json());row.addProperty("witness_snbt",stack.getTag().toString());row.addProperty("full_nbt_callback_cache",callback);row.add("aspects",values(actual));variants.add(row);
                variantCsv.append(id).append(',').append(csv(predicate.canonical())).append(',').append(csv(stack.getTag().toString())).append(',').append(csv(values(actual).toString())).append('\n');
            }
        }
        root.add("nbt_variants",variants);root.addProperty("nbt_cache_parity",true);root.addProperty("nbt_variant_scope","One concrete witness per loaded NBT selector; arbitrary NBT and overlapping-selector combinations are not exhaustively enumerated.");
        var blocks=new JsonArray();var blockCsv=new StringBuilder("id,state,item,source,aspects\n");
        for(var id:ForgeRegistries.BLOCKS.getKeys().stream().sorted().toList()){
            var block=ForgeRegistries.BLOCKS.getValue(id);var row=new JsonObject();row.addProperty("id",id.toString());
            var item=block.asItem();String itemId=ForgeRegistries.ITEMS.getKey(item).toString();row.addProperty("item",itemId);
            var states=new JsonArray();
            for(var state:block.getStateDefinition().getPossibleStates()){
                var value=new JsonObject();var properties=new JsonObject();BlockScanTargets.properties(state).forEach(properties::addProperty);value.add("properties",properties);
                var mapped=BlockScanTargets.target(state);String mode=mapped.isPresent()?"mapping":item!=Items.AIR?"item":"empty";
                var explicit=BlockAspects.resolve(state);if(explicit.isPresent())mode="explicit";
                if(!explicit.map(AspectTableExportCommand::values).equals(local.blockAspects().resolve(id.toString(),BlockScanTargets.properties(state)).map(AspectTableExportCommand::values)))throw new IllegalStateException("Client/server block definition mismatch: "+state);
                var amounts=explicit.orElseGet(()->mapped.isPresent()?BlockScanTargets.aspects(mapped.get()):item!=Items.AIR?ItemStackAspects.resolve(new ItemStack(item)).orElseGet(AspectAmounts::new):new AspectAmounts());
                value.addProperty("source",mode);value.add("aspects",values(amounts));states.add(value);
                blockCsv.append(id).append(',').append(csv(properties.toString())).append(',').append(itemId).append(',').append(mode).append(',').append(csv(value.get("aspects").toString())).append('\n');
            }
            row.add("states",states);blocks.add(row);
        }
        root.add("blocks",blocks);root.addProperty("block_scope","All registered states; static scan mappings/item fallback. Live node/container/custom world callbacks are not materialized.");
        root.addProperty("client_evaluator_parity",true);root.addProperty("sync_frames",frames.size());
        var gson=new GsonBuilder().setPrettyPrinting().create();Files.writeString(dir.resolve("table.json"),gson.toJson(root),StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("items.csv"),itemCsv,StandardCharsets.UTF_8);Files.writeString(dir.resolve("blocks.csv"),blockCsv,StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("nbt-variants.csv"),variantCsv,StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("resources.json"),gson.toJson(tc4revival.aspect.sync.ServerAspectData.snapshot().files()),StandardCharsets.UTF_8);
        source.sendSuccess(()->Component.literal("ASPECT_EXPORT_OK "+items.size()+" items, "+blocks.size()+" blocks: "+dir.toAbsolutePath()),false);return items.size();
    }
    private static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
}
