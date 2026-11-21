package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/** Exact column builder; rarity and shared aura/structure scheduling belong to the world-generation dispatcher. */
public  class ObsidianTotemFeature extends Feature<NoneFeatureConfiguration> {
    public ObsidianTotemFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var origin=context.origin();int y=LegacySurfaceHeight.at(context.level(),origin.getX(),origin.getZ())-1;
        return generate(context.level(),new BlockPos(origin.getX(),y,origin.getZ()),context.random());
    }
    private static boolean clear(BlockState state) { return state.isAir()||state.is(Blocks.SNOW)||state.is(Blocks.GRASS)||state.is(Blocks.FERN); }
    public static boolean generate(WorldGenLevel level,BlockPos ground,RandomSource random) {
        if(level.getBlockState(ground).is(BlockTags.LEAVES))while(ground.getY()>level.getMinBuildHeight()) { ground=ground.below();if(level.getBlockState(ground).is(Blocks.GRASS_BLOCK))break; }
        if(level.getBlockState(ground).is(Blocks.SNOW)||level.getBlockState(ground).is(Blocks.GRASS)||level.getBlockState(ground).is(Blocks.FERN))ground=ground.below();
        var soil=level.getBlockState(ground);
        if(!(soil.is(Blocks.GRASS_BLOCK)||soil.is(Blocks.SAND)||soil.is(Blocks.DIRT)||soil.is(Blocks.STONE)||soil.is(Blocks.NETHERRACK))||!clear(level.getBlockState(ground.above())))return false;
        if(level.isOutsideBuildHeight(ground)||level.isOutsideBuildHeight(ground.above(5)))return false;
        level.setBlock(ground,TCEldritch.OBSIDIAN_TILE.get().defaultBlockState(),3);
        for(int count=1;count<5&&clear(level.getBlockState(ground.above(count)));count++) {
            var pos=ground.above(count);level.setBlock(pos,TCArcane.OBSIDIAN_TOTEM.get().at(level,pos),3);
            if(count>1&&random.nextInt(4)==0) { placeNode(level,pos,random);return true; }
            if(count==4 && clear(level.getBlockState(ground.above(5))))placeNode(level,ground.above(5),random);
        }
        return true;
    }
    private static void placeNode(WorldGenLevel level,BlockPos pos,RandomSource random) {
        level.setBlock(pos,TCArcane.OBSIDIAN_NODE_TOTEM.get().at(level,pos),3);AuraNodeGeneration.createRandom(level,pos,random,false,true,false);
    }
}
