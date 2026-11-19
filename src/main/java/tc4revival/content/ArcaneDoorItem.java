package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.properties.*;

public  class ArcaneDoorItem extends BlockItem {
    public ArcaneDoorItem() { super(TCArcane.ARCANE_DOOR.get(),new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel(); var player=context.getPlayer(); var pos=context.getClickedPos().above();
        if (player==null || context.getClickedFace()!=Direction.UP || pos.getY()+1>=level.getMaxBuildHeight()
                || !level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)) return InteractionResult.FAIL;
        for (var target:java.util.List.of(pos,pos.above())) if (!level.getBlockState(target).canBeReplaced()
                || !level.mayInteract(player,target) || !player.mayUseItemAt(target,Direction.UP,context.getItemInHand())) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int orientation=net.minecraft.util.Mth.floor((player.getYRot()+180)*4.0/360-.5)&3;
        var facing=new Direction[]{Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.NORTH}[orientation];
        var right=facing.getClockWise(); int leftCount=0,rightCount=0; boolean leftDoor=false,rightDoor=false;
        for (int y=0;y<2;y++) {
            var left=pos.above(y).relative(right.getOpposite()); var rpos=pos.above(y).relative(right);
            if (level.getBlockState(left).isRedstoneConductor(level,left)) leftCount++;
            if (level.getBlockState(rpos).isRedstoneConductor(level,rpos)) rightCount++;
            leftDoor|=level.getBlockState(left).is(getBlock()); rightDoor|=level.getBlockState(rpos).is(getBlock());
        }
        var state=getBlock().defaultBlockState().setValue(ArcaneDoorBlock.FACING,facing)
                .setValue(ArcaneDoorBlock.HINGE,leftDoor && !rightDoor || rightCount>leftCount ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        var original=level.getBlockState(pos); var originalUpper=level.getBlockState(pos.above());
        boolean placed=false;
        try {
            if (!level.setBlock(pos,state,18) || !level.setBlock(pos.above(),state.setValue(ArcaneDoorBlock.HALF,DoubleBlockHalf.UPPER),18)) return InteractionResult.FAIL;
            for (var target:java.util.List.of(pos,pos.above())) {
                if (!(level.getBlockEntity(target) instanceof OwnedDeviceBlockEntity owned)) return InteractionResult.FAIL;
                owned.access().setOwner(player.getGameProfile().getName()); owned.changed();
            }
            placed=true;
        } finally {
            if (!placed) { level.setBlock(pos,original,18); level.setBlock(pos.above(),originalUpper,18); }
        }
        level.updateNeighborsAt(pos,getBlock()); level.updateNeighborsAt(pos.above(),getBlock());
        if (!player.isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
