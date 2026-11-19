package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
/** Old height map follows light opacity; transparent nodes and plants do not raise the surface. */
public  class LegacySurfaceHeight {
    private LegacySurfaceHeight() {}
    /** From CARVERS onward (including decoration), WG heightmaps stop tracking new blocks. */
    static Heightmap.Types mapFor(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        return chunk instanceof net.minecraft.world.level.chunk.LevelChunk
                || chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk
                || chunk.getStatus().heightmapsAfter().contains(Heightmap.Types.WORLD_SURFACE)
                ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
    }
    static int nonAirTop(WorldGenLevel level,int x,int z) {
        return level.getHeight(mapFor(level.getChunk(x>>4,z>>4)),x,z);
    }
    public static int at(WorldGenLevel level,int x,int z){
        int y=nonAirTop(level,x,z)-1;
        var pos=new BlockPos.MutableBlockPos(x,y,z);
        while(y>=level.getMinBuildHeight()){pos.setY(y);if(level.getBlockState(pos).getLightBlock(level,pos)>0)return y+1;y--;}
        return level.getMinBuildHeight();
    }
}
