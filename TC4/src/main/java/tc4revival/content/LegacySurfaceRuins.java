package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.EntityType;
/** Common old terrain tests and immediate dungeon loot/spawner initialization. */
public  class LegacySurfaceRuins {
    private LegacySurfaceRuins() {}
    private static boolean soil(net.minecraft.world.level.block.state.BlockState state) { return state.is(Blocks.STONE)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT); }
    public static boolean valid(net.minecraft.world.level.WorldGenLevel level,BlockPos pos) {
        int distance=0;while(!level.isEmptyBlock(pos.above(distance)))if(++distance>2)return false;
        var floor=pos.above(distance-1);var state=level.getBlockState(floor);
        return soil(state)||(state.is(Blocks.SNOW)||state.is(Blocks.GRASS)||state.is(Blocks.FERN))&&soil(level.getBlockState(floor.below()));
    }
    public static void chest(net.minecraft.world.level.WorldGenLevel level,BlockPos pos,RandomSource random) {
        chest(level,pos,random,false);
    }
    public static void chest(net.minecraft.world.level.WorldGenLevel level,BlockPos pos,RandomSource random,boolean doubled) {
        if(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)chest.setLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:chests/"+(doubled?"dungeon_double":"dungeon")),random.nextLong());
    }
    public static void spawner(net.minecraft.world.level.WorldGenLevel level,BlockPos pos,EntityType<?> type,RandomSource random) {
        level.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),3);
        if(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)spawner.setEntityId(type,random);
    }
}
