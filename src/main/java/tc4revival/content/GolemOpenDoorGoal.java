package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.*;

/** Native open/close lifecycle extended to the doors and gates supported by golem navigation. */
public class GolemOpenDoorGoal extends OpenDoorGoal {
    private final GolemWorkHost host;
    public GolemOpenDoorGoal(GolemWorkHost host) { super(host.mob(), true); this.host = host; }
    @Override public boolean canUse() {
        if (!host.workEnabled()) return false;
        if (super.canUse()) return true;
        if (!mob.horizontalCollision || !(mob.getNavigation() instanceof GroundPathNavigation navigation)) return false;
        var path = navigation.getPath();
        if (path == null || path.isDone() || !navigation.canOpenDoors()) return false;
        for (int i = Math.max(0, path.getNextNodeIndex() - 1); i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
            var node = path.getNode(i);
            var pos = new BlockPos(node.x,node.y,node.z);
            if (mob.distanceToSqr(node.x,mob.getY(),node.z) <= 2.25 && GolemDoorNavigation.usable(mob.level().getBlockState(pos))) {
                doorPos = pos; hasDoor = true; return true;
            }
        }
        return false;
    }
    @Override public boolean canContinueToUse() { return host.workEnabled() && super.canContinueToUse(); }
    @Override protected void setOpen(boolean open) {
        var level = mob.level(); var state = level.getBlockState(doorPos);
        if (state.getBlock() instanceof DoorBlock) { super.setOpen(open); return; }
        if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN) != open) {
            if (open && state.getValue(FenceGateBlock.FACING) == mob.getDirection().getOpposite())
                state = state.setValue(FenceGateBlock.FACING, mob.getDirection());
            level.setBlock(doorPos, state.setValue(FenceGateBlock.OPEN,open),3);
            level.playSound(null,doorPos,open?SoundEvents.FENCE_GATE_OPEN:SoundEvents.FENCE_GATE_CLOSE,SoundSource.BLOCKS,1,1);
            level.gameEvent(mob,open?net.minecraft.world.level.gameevent.GameEvent.BLOCK_OPEN:net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE,doorPos);
        }
    }
}
