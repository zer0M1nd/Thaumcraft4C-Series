package heartwork.content;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import heartwork.Heartwork;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.*;

public final class HeartworkVillage {
    public static final RegistryObject<Block> ANVIL=Heartwork.BLOCKS.register("smithing_anvil",() -> new Block(BlockBehaviour.Properties.copy(Blocks.ANVIL)));
    public static final RegistryObject<Block> HEARTH=Heartwork.BLOCKS.register("cooking_hearth",() -> new Block(BlockBehaviour.Properties.copy(Blocks.FURNACE).lightLevel(state -> 0)));
    public static final DeferredRegister<PoiType> POIS=DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE,Heartwork.ID);
    public static final RegistryObject<PoiType> SMITH_POI=POIS.register("bladesmith",() -> new PoiType(Set.copyOf(ANVIL.get().getStateDefinition().getPossibleStates()),1,1));
    public static final RegistryObject<PoiType> COOK_POI=POIS.register("heartworker",() -> new PoiType(Set.copyOf(HEARTH.get().getStateDefinition().getPossibleStates()),1,1));
    public static final DeferredRegister<VillagerProfession> PROFESSIONS=DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS,Heartwork.ID);
    public static final RegistryObject<VillagerProfession> SMITH=PROFESSIONS.register("bladesmith",() -> new VillagerProfession("heartwork.bladesmith",h -> h.is(SMITH_POI.getId()),h -> h.is(SMITH_POI.getId()),ImmutableSet.of(),ImmutableSet.of(),SoundEvents.VILLAGER_WORK_WEAPONSMITH));
    public static final RegistryObject<VillagerProfession> COOK=PROFESSIONS.register("heartworker",() -> new VillagerProfession("heartwork.heartworker",h -> h.is(COOK_POI.getId()),h -> h.is(COOK_POI.getId()),ImmutableSet.of(),ImmutableSet.of(),SoundEvents.VILLAGER_WORK_BUTCHER));
    public static final net.minecraftforge.common.ForgeConfigSpec CONFIG;
    public static final net.minecraftforge.common.ForgeConfigSpec.IntValue HOUSE_WEIGHT;
    static {
        var builder=new net.minecraftforge.common.ForgeConfigSpec.Builder();
        HOUSE_WEIGHT=builder.comment("Weight of each Heartwork house in modern village house pools. Zero disables new generation.","The legacy weight of 50 belonged to a different village algorithm with per-house limits.").defineInRange("villageHouseWeight",2,0,50);
        CONFIG=builder.build();
    }
    public static void register(IEventBus bus){
        Heartwork.ITEMS.register("smithing_anvil",() -> new BlockItem(ANVIL.get(),new Item.Properties()));
        Heartwork.ITEMS.register("cooking_hearth",() -> new BlockItem(HEARTH.get(),new Item.Properties()));
        POIS.register(bus);PROFESSIONS.register(bus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,CONFIG);
        MinecraftForge.EVENT_BUS.addListener(HeartworkVillage::trades);
        MinecraftForge.EVENT_BUS.addListener(HeartworkVillage::addHouses);
        MinecraftForge.EVENT_BUS.addListener(HeartworkVillage::chestLoot);
    }
    public static void trades(VillagerTradesEvent event){
        if(event.getType()==COOK.get()){
            String[] items={"sweetpotato_baked","pear","biscuit","marshmallows","butter","cheese","toast"};
            int[] prices={6,4,6,6,4,4,6},counts={4,1,8,6,4,2,6};
            for(int i=0;i<items.length;i++){int n=i;event.getTrades().get(1+i/2).add((entity,random) -> new MerchantOffer(new ItemStack(Items.EMERALD,prices[n]),new ItemStack(Heartwork.item(items[n]),counts[n]),12,2+5*(n/2),0.05F));}
        } else if(event.getType()==SMITH.get()&&ModList.get().isLoaded("slashblade"))heartwork.compat.blade.BladeTrades.add(event);
    }
    /** Add to the existing pool; retain vanilla and other mods' elements and weights. */
    @SuppressWarnings("unchecked") public static void addHouses(ServerAboutToStartEvent event){
        if(HOUSE_WEIGHT.get()==0)return;
        var registry=event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"}){
            var pool=registry.get(new ResourceLocation("minecraft:village/"+biome+"/houses"));if(pool==null)continue;
            try {
                var rawField=ObfuscationReflectionHelper.findField(StructureTemplatePool.class,"f_210559_");
                var entries=new ArrayList<>((List<Pair<StructurePoolElement,Integer>>)rawField.get(pool));
                var expanded=(List<StructurePoolElement>)ObfuscationReflectionHelper.findField(StructureTemplatePool.class,"f_210560_").get(pool);
                for(String house:new String[]{"heartworker","bladesmith"}){
                    if(house.equals("bladesmith")&&!ModList.get().isLoaded("slashblade"))continue;
                    var element=StructurePoolElement.single("heartwork:village/"+biome+"/"+house).apply(StructureTemplatePool.Projection.RIGID);
                    entries.add(Pair.of(element,HOUSE_WEIGHT.get()));for(int i=0;i<HOUSE_WEIGHT.get();i++)expanded.add(element);
                }
                rawField.set(pool,entries);ObfuscationReflectionHelper.findField(StructureTemplatePool.class,"f_210562_").setInt(pool,Integer.MIN_VALUE);
            } catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot extend village pool "+biome,ex);}
        }
    }
    public static void chestLoot(net.minecraftforge.event.LootTableLoadEvent event){
        if(!ModList.get().isLoaded("slashblade"))return;
        String id=event.getName().toString();int a,b;
        if(id.equals("minecraft:chests/village/village_weaponsmith")||id.equals("minecraft:chests/jungle_temple")){a=10;b=4;}
        else if(id.equals("minecraft:chests/stronghold_corridor")||id.equals("minecraft:chests/stronghold_crossing")){a=16;b=4;}
        else if(id.equals("minecraft:chests/simple_dungeon")){a=16;b=10;}else return;
        // Retain the actual pool, including other mods' functions and roll counts.
        // Re-parsing a table here is invalid: Forge has already popped its JSON context.
        try {
            var pools=(java.util.List<net.minecraft.world.level.storage.loot.LootPool>)ObfuscationReflectionHelper.findField(net.minecraft.world.level.storage.loot.LootTable.class,"f_79109_").get(event.getTable());
            if(pools.isEmpty())return;
            var field=ObfuscationReflectionHelper.findField(net.minecraft.world.level.storage.loot.LootPool.class,"f_79023_");
            var original=(net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[])field.get(pools.get(0));
            var entries=java.util.Arrays.copyOf(original,original.length+2);
            entries[original.length]=net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(Heartwork.item("book1")).setWeight(a).build();
            entries[original.length+1]=net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(Heartwork.item("book2")).setWeight(b).build();
            field.set(pools.get(0),entries);
        } catch(ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot extend Heartwork chest loot "+id,ex);
        }
    }
}
