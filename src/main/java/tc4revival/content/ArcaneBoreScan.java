package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Stateful TC4 spiral, including the two-block advance before the first depth probe. */
public  class ArcaneBoreScan {
    private int spiral;
    private float radius,increment;
    private BlockPos last=BlockPos.ZERO;
    public record Target(BlockPos pos,int delay) {}

    public void reorient() {
        last=BlockPos.ZERO; spiral=0; radius=0; increment=0;
    }
    public BlockPos nextColumn(BlockPos origin,Direction direction,int area) {
        int maximum=2+area;
        if (maximum<=0) throw new IllegalArgumentException("Bore radius must be positive");
        if (increment==0) increment=maximum/360.0f;
        BlockPos column;
        do {
            spiral=(spiral+2)%360; radius+=increment;
            if (radius>maximum || radius<-maximum) increment=-increment;
            // TCVec3 uses the opposite Z rotation sign from modern Vec3.zRot.
            float angle=spiral/180.0f*(float)Math.PI;
            double x=radius*(double)Mth.sin(angle),y=radius*(double)Mth.cos(angle),z=0;
            angle=1.5707964f*direction.getStepX();
            double rotatedX=x*Mth.cos(angle)+z*Mth.sin(angle);
            z=z*Mth.cos(angle)-x*Mth.sin(angle); x=rotatedX;
            angle=1.5707964f*direction.getStepY();
            double rotatedY=y*Mth.cos(angle)+z*Mth.sin(angle);
            z=z*Mth.cos(angle)-y*Mth.sin(angle); y=rotatedY;
            column=BlockPos.containing(origin.getX()+direction.getStepX()+.5+x,
                    origin.getY()+direction.getStepY()+.5+y,origin.getZ()+direction.getStepZ()+.5+z);
        } while (column.equals(last));
        last=column; return column;
    }
    public Target find(ServerLevel level,BlockPos origin,Direction direction,int area,int speed,float acceleration) {
        var column=nextColumn(origin,direction,area).relative(direction);
        for (int depth=0;depth<64;depth++) {
            column=column.relative(direction);
            if (level.isOutsideBuildHeight(column) || !level.hasChunkAt(column)) break;
            var state=level.getBlockState(column); float hardness=state.getDestroySpeed(level,column);
            if (hardness<0) break;
            if (state.isAir() || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock || state.getCollisionShape(level,column).isEmpty()) continue;
            var target=column;
            var start=Vec3.atCenterOf(origin.relative(direction));
            var hit=level.clip(new ClipContext(start,Vec3.atCenterOf(target),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,null));
            if (hit.getType()!=HitResult.Type.MISS) {
                var obstruction=hit.getBlockPos();
                var obstructionState=level.getBlockState(obstruction);
                float obstructionHardness=obstructionState.getDestroySpeed(level,obstruction);
                if (obstructionHardness>-1 && !obstructionState.getCollisionShape(level,obstruction).isEmpty()) {
                    target=obstruction; hardness=obstructionHardness;
                }
            }
            return new Target(target,delay(hardness,speed,acceleration));
        }
        return null;
    }
    public static int delay(float hardness,int speed,float acceleration) {
        int delay=Math.max(10-speed,(int)(hardness*2)-speed*2);
        return acceleration<1?delay*4:delay;
    }
}
