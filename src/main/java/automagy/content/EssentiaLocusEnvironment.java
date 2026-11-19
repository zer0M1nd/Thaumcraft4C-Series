package automagy.content;

import automagy.logic.EssentiaLocusStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import static automagy.logic.EssentiaLocusStructure.Cell.*;

public final class EssentiaLocusEnvironment implements EssentiaLocusStructure.View {
    public static final TagKey<Block> SILVERWOOD=tag("locus_silverwood"),WATER_CLUSTER=tag("locus_water_clusters"),AGGREGATOR=tag("locus_aggregators");
    private final Level level;private final BlockPos origin;
    public EssentiaLocusEnvironment(Level level,BlockPos origin){this.level=level;this.origin=origin.immutable();}
    @Override public EssentiaLocusStructure.Cell at(int x,int y,int z){
        var pos=origin.offset(x,y,z);if(level.isOutsideBuildHeight(pos))return OTHER;if(!level.hasChunkAt(pos))return UNKNOWN;
        var state=level.getBlockState(pos);if(state.isAir())return AIR;
        if(state.is(Blocks.WATER))return state.getFluidState().isSource()?SOURCE_WATER:FLOWING_WATER;
        if(state.is(Blocks.LAPIS_BLOCK))return LAPIS;
        if(state.is(SILVERWOOD))return EssentiaLocusStructure.Cell.SILVERWOOD;
        if(state.is(WATER_CLUSTER))return EssentiaLocusStructure.Cell.WATER_CLUSTER;
        if(state.is(AGGREGATOR))return EssentiaLocusStructure.Cell.AGGREGATOR;
        return OTHER;
    }
    private static TagKey<Block> tag(String name){return TagKey.create(Registries.BLOCK,ResourceLocation.parse("automagy:"+name));}
}
