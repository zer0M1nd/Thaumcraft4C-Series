package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class LegacySurfaceHeightServerTests {
    @GameTest(template="wand_test_empty")
    public static void legacySurfaceIgnoresStaleGenerationMap(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(2,2,2)).atY(90);var chunk=level.getChunkAt(base);
        var oldBase=level.getBlockState(base);var oldGlass=level.getBlockState(base.above());var oldStone=level.getBlockState(base.above(2));
        try {
            // Freeze a real generation heightmap before changing a fully loaded chunk.
            Heightmap.primeHeightmaps(chunk,java.util.Set.of(Heightmap.Types.WORLD_SURFACE_WG));
            int stale=chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG,base.getX()&15,base.getZ()&15);
            level.setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(base.above(),Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(base.above(2),Blocks.AIR.defaultBlockState());
            test.assertTrue(chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG,base.getX()&15,base.getZ()&15)==stale,"Live edits leave generation map stale");
            test.assertTrue(LegacySurfaceHeight.at(level,base.getX(),base.getZ())==91,"Opaque surface excludes transparent glass above it");
            level.setBlockAndUpdate(base.above(2),Blocks.STONE.defaultBlockState());
            test.assertTrue(LegacySurfaceHeight.at(level,base.getX(),base.getZ())==93,"Live height follows new opaque blocks");
            test.assertTrue(LegacySurfaceHeight.mapFor(new ImposterProtoChunk(chunk,false))==Heightmap.Types.WORLD_SURFACE,"Wrapped full chunks use live map");
            var proto=new ProtoChunk(chunk.getPos(),UpgradeData.EMPTY,level,level.registryAccess().registryOrThrow(Registries.BIOME),null);
            test.assertTrue(LegacySurfaceHeight.mapFor(proto)==Heightmap.Types.WORLD_SURFACE_WG,"Unfinished terrain uses generation map");
        } finally {
            level.setBlockAndUpdate(base,oldBase);level.setBlockAndUpdate(base.above(),oldGlass);level.setBlockAndUpdate(base.above(2),oldStone);
        }
        com.mojang.logging.LogUtils.getLogger().info("Legacy surface test passed: live-generation-map-opacity");test.succeed();
    }
}
