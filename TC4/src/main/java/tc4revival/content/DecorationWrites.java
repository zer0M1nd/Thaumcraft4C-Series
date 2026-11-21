package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/** FEATURES may read a larger region than their one-chunk write radius. */
public class DecorationWrites {
    public static boolean allowed(WorldGenLevel level,BlockPos pos) {
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return false;
        if(level instanceof net.minecraft.server.level.WorldGenRegion region) {
            var center=region.getCenter();
            if(Math.abs((pos.getX()>>4)-center.x)>1||Math.abs((pos.getZ()>>4)-center.z)>1)return false;
        }
        return level.ensureCanWrite(pos);
    }
    public static boolean set(WorldGenLevel level,BlockPos pos,BlockState state,int flags) {
        return allowed(level,pos)&&level.setBlock(pos,state,flags);
    }
    private DecorationWrites(){}
}
