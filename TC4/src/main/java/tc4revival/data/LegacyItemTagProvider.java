package tc4revival.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.content.ShardType;
import tc4revival.content.TCItems;

public  class LegacyItemTagProvider extends IntrinsicHolderTagsProvider<Item> {
    public LegacyItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, Registries.ITEM, lookup, item -> ForgeRegistries.ITEMS.getResourceKey(item).orElseThrow(), TC4Revival.MOD_ID, helper);
    }

    @Override protected void addTags(HolderLookup.Provider lookup) {
        TCItems.PRIMAL_ARROWS.forEach(arrow->tag(ItemTags.ARROWS).add(arrow.get()));
        tag(ItemTags.AXES).add(TCItems.ELEMENTAL_AXE.get());
        var repairable=tag(tc4revival.content.LegacyRepairEnchantment.REPAIRABLE);
        var hotbarRepair=tag(tc4revival.content.LegacyRepairEnchantment.HOTBAR);
        for(var item:ForgeRegistries.ITEMS)if(item instanceof tc4revival.content.RepairableItem repair) {
            repairable.add(item);if(repair.allowHotbarRepair())hotbarRepair.add(item);
        }
        tag(ItemTags.create(ResourceLocation.parse("forge:storage_blocks"))).add(tc4revival.content.TCArcane.THAUMIUM_BLOCK.get().asItem());
        for(int ore=0;ore<8;ore++)for(var block:java.util.List.of(tc4revival.content.TCArcane.ore(ore),tc4revival.content.TCArcane.deepslateOre(ore))){var item=block.asItem();tag(ItemTags.create(ResourceLocation.parse(tc4revival.content.LegacyOreBlock.tag(ore)))).add(item);tag(ItemTags.create(ResourceLocation.parse("forge:ores"))).add(item);}
        for (int i=0;i<16;i++) tag(ItemTags.create(ResourceLocation.parse("thaumcraft:tallow_candles"))).add(TCItems.candle(i));
        for (String name:tc4revival.content.TCArcane.woodworkNames()) tag(name.endsWith("planks") ? ItemTags.PLANKS : name.endsWith("slab") ? ItemTags.WOODEN_SLABS : ItemTags.WOODEN_STAIRS).add(TCItems.woodwork(name));
        tag(ItemTags.LOGS).add(TCItems.GREATWOOD_LOG.get(),TCItems.SILVERWOOD_LOG.get());
        tag(ItemTags.LOGS_THAT_BURN).add(TCItems.GREATWOOD_LOG.get(),TCItems.SILVERWOOD_LOG.get());
        tag(ItemTags.LEAVES).add(TCItems.GREATWOOD_LEAVES.get(),TCItems.SILVERWOOD_LEAVES.get());
        tag(ItemTags.SAPLINGS).add(TCItems.GREATWOOD_SAPLING.get(),TCItems.SILVERWOOD_SAPLING.get());
        for (var type:tc4revival.content.RunicBaubleType.values()) tag(ItemTags.create(ResourceLocation.parse("curios:"+type.slot()))).add(TCItems.runicBauble(type));
        for (var type : tc4revival.content.ElementalRingType.values()) tag(ItemTags.create(ResourceLocation.parse("curios:ring"))).add(TCItems.elementalRing(type));
        tag(ItemTags.create(ResourceLocation.parse("curios:necklace"))).add(TCItems.LESSER_VIS_AMULET.get(), TCItems.VIS_AMULET.get());
        tag(ItemTags.create(ResourceLocation.parse("curios:head"))).add(TCItems.GOGGLES.get());
        tag(ItemTags.SLABS).add(TCItems.ARCANE_STONE_SLAB.get()); tag(ItemTags.STAIRS).add(TCItems.ARCANE_STONE_STAIRS.get());
        for (var type : tc4revival.content.MundaneBaubleType.values())
            tag(ItemTags.create(ResourceLocation.parse("curios:" + type.slot()))).add(TCItems.bauble(type));
        for (var type : tc4revival.content.ClusterType.values()) {
            var category = ItemTags.create(ResourceLocation.parse(type.tag())); tag(category).add(TCItems.cluster(type));
            tag(ItemTags.create(ResourceLocation.parse("forge:clusters"))).addTag(category);
        }
        for (var type : tc4revival.content.NuggetType.values()) type.tag().ifPresent(name -> {
            var category = ItemTags.create(ResourceLocation.parse(name)); tag(category).add(TCItems.nugget(type));
            tag(ItemTags.create(ResourceLocation.parse("forge:nuggets"))).addTag(category);
        });
        for (var type : tc4revival.content.MaterialType.values()) for (String name : type.tags()) {
            var category = ItemTags.create(ResourceLocation.parse(name)); tag(category).add(TCItems.material(type));
            int separator = name.indexOf('/');
            if (separator > 0) tag(ItemTags.create(ResourceLocation.parse(name.substring(0, separator)))).addTag(category);
        }
        tag(ItemTags.create(ResourceLocation.parse("curios:belt"))).add(TCItems.FOCUS_POUCH.get(),TCItems.HOVER_GIRDLE.get());
        tag(ItemTags.create(ResourceLocation.parse("forge:raw_fishes"))).add(net.minecraft.world.item.Items.COD,net.minecraft.world.item.Items.SALMON,net.minecraft.world.item.Items.TROPICAL_FISH,net.minecraft.world.item.Items.PUFFERFISH);
        var allShards = tag(ItemTags.create(ResourceLocation.parse("thaumcraft:shards")));
        var oreShards = tag(ItemTags.create(ResourceLocation.parse("forge:shards")));
        for (var type : ShardType.values()) {
            allShards.add(TCItems.shard(type));
            if (type == ShardType.BALANCED) continue;
            var category = ItemTags.create(ResourceLocation.parse("forge:shards/" + type.material()));
            tag(category).add(TCItems.shard(type));
            oreShards.addTag(category);
        }
    }
}
