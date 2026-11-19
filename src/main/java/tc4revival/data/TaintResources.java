package tc4revival.data;

import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import tc4revival.content.TCArcane;
import tc4revival.content.TaintFibreBlock;

public  class TaintResources extends BlockStateProvider {
    public TaintResources(PackOutput output,ExistingFileHelper helper) { super(output,"thaumcraft",helper); }
    @Override protected void registerStatesAndModels() {
        simpleBlockWithItem(TCArcane.TAINT_CRUST.get(),models().cubeAll("taint_crust",modLoc("block/taint_crust")));
        simpleBlockWithItem(TCArcane.FLESH_BLOCK.get(),models().cubeAll("flesh_block",modLoc("block/fleshblock")));
        var soil=models().getBuilder("taint_soil").parent(models().getExistingFile(mcLoc("block/block"))).texture("particle",modLoc("block/taint_soil")).texture("all",modLoc("block/taint_soil"));
        soil.element().from(0,0,0).to(16,16,16).allFaces((side,face)->face.texture("#all").tintindex(0).cullface(side)).end();
        simpleBlockWithItem(TCArcane.TAINT_SOIL.get(),soil);
        var parts=getMultipartBuilder(TCArcane.TAINT_FIBRES.get());
        for (Direction side:Direction.values()) {
            var model=models().getBuilder("taint_fibres_"+side.getName()).renderType("minecraft:translucent").texture("particle",modLoc("block/taint_fibres")).texture("all",modLoc("block/taint_fibres"));
            float[] from={0,0,0},to={16,16,16}; int axis=side.getAxis()==Direction.Axis.X ? 0 : side.getAxis()==Direction.Axis.Y ? 1 : 2;
            from[axis]=to[axis]=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE ? .08f : 15.92f;
            model.element().from(from[0],from[1],from[2]).to(to[0],to[1],to[2])
                    .face(side.getOpposite()).texture("#all").end().end();
            parts.part().modelFile(model).addModel().condition(MultifaceBlock.getFaceProperty(side),true).end();
        }
        for(int overlay=1;overlay<=3;overlay++) models().cubeAll("taint_overlay_sprite_"+overlay,modLoc("block/taint_over_"+overlay));
        String[] textures={"taintgrass1","taintgrass2","taint_spore_stalk_1","taint_spore_stalk_2"};
        for (int i=0;i<textures.length;i++) parts.part().modelFile(models().cross("taint_growth_"+(i+1),modLoc("block/"+textures[i])).renderType("minecraft:translucent"))
                .addModel().condition(TaintFibreBlock.VARIANT,i+1).end();
    }
    public static LootTableProvider loot(PackOutput output) {
        return new LootTableProvider(output,Set.of(),List.of(new LootTableProvider.SubProviderEntry(Drops::new,LootContextParamSets.BLOCK)));
    }
    private static  class Drops extends BlockLootSubProvider {
        Drops() { super(Set.of(),FeatureFlags.REGISTRY.allFlags()); }
        @Override protected Iterable<Block> getKnownBlocks() { return List.of(TCArcane.TAINT_CRUST.get(),TCArcane.TAINT_SOIL.get(),TCArcane.FLESH_BLOCK.get()); }
        @Override protected void generate() {
            add(TCArcane.TAINT_CRUST.get(),block->createSilkTouchDispatchTable(block,EmptyLootItem.emptyItem()));
            add(TCArcane.TAINT_SOIL.get(),block->createSingleItemTableWithSilkTouch(block,net.minecraft.world.level.block.Blocks.DIRT));
            add(TCArcane.FLESH_BLOCK.get(),block->createSilkTouchDispatchTable(block,LootItem.lootTableItem(Items.ROTTEN_FLESH)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(9)))));
        }
    }
}
