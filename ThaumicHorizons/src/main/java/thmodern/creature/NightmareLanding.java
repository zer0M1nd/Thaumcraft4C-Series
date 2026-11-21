package thmodern.creature;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Small bounded destination search, never creating portals or excavating a landing chamber. */
public final class NightmareLanding {
    private NightmareLanding(){}
    public static Vec3 find(ServerLevel level,double x,double y,double z){
        var center=level.getWorldBorder().clampToBounds(x,y,z);Vec3 best=null;double distance=Double.MAX_VALUE;
        int top=Math.min(level.getMaxBuildHeight()-4,level.getMinBuildHeight()+level.dimensionType().logicalHeight()-4);
        for(int dx=-16;dx<=16;dx++)for(int dz=-16;dz<=16;dz++){
            int bx=center.getX()+dx,bz=center.getZ()+dz;var column=new BlockPos(bx,center.getY(),bz);
            if(!level.getWorldBorder().isWithinBounds(column))continue;
            // Dimension travel may generate its destination, like a vanilla portal transfer.
            level.getChunk(bx>>4,bz>>4);
            for(int by=top;by>level.getMinBuildHeight();by--){
                double score=(bx+.5-x)*(bx+.5-x)+(by-y)*(by-y)+(bz+.5-z)*(bz+.5-z);if(score>=distance)continue;
                var pos=new BlockPos(bx,by,bz);if(!safe(level,pos))continue;best=new Vec3(bx+.5,by,bz+.5);distance=score;
            }
        }return best;
    }
    public static boolean safe(ServerLevel level,BlockPos pos){
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            var foot=pos.offset(dx,0,dz);if(!level.hasChunkAt(foot)||!level.getWorldBorder().isWithinBounds(foot)||!level.getBlockState(foot.below()).isFaceSturdy(level,foot.below(),Direction.UP))return false;
            for(int dy=0;dy<4;dy++)if(!level.isEmptyBlock(foot.above(dy)))return false;
        }
        return level.noCollision(new net.minecraft.world.phys.AABB(pos.getX()-1,pos.getY(),pos.getZ()-1,pos.getX()+2,pos.getY()+4,pos.getZ()+2));
    }
}
