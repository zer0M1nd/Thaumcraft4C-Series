package tc4revival.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Called after structure triggers and before focus handling, as in the old wand. */
public  class OwnedDeviceRecovery {
    private OwnedDeviceRecovery() {}
    public static InteractionResult recover(UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var state=level.getBlockState(pos);
        if (state.is(TCArcane.ARCANE_DOOR.get())) return recoverDoor(context,ArcaneDoorBlock.root(state,pos));
        boolean glass=state.is(TCArcane.WARDED_GLASS.get());
        boolean plate=state.is(TCArcane.ARCANE_PRESSURE_PLATE.get()) && !state.getValue(ArcanePressurePlateBlock.POWERED);
        if (!glass && !plate) return InteractionResult.PASS;
        var player=context.getPlayer();
        if (player==null || !(level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity entity)) return InteractionResult.PASS;
        if (tc4revival.config.TCConfig.SERVER.wardedStone.get() && !entity.access().ownedBy(player.getGameProfile().getName())) return InteractionResult.PASS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var saved=entity.saveWithFullMetadata();
        if (!level.removeBlock(pos,false)) return InteractionResult.FAIL;
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(glass ? TCItems.WARDED_GLASS.get() : TCItems.ARCANE_PRESSURE_PLATE.get()));
        if (!level.addFreshEntity(drop)) {
            level.setBlock(pos,state,3);
            var restored=level.getBlockEntity(pos);
            if (restored==null || !level.getBlockState(pos).equals(state)) throw new IllegalStateException("Cannot restore recovered warded glass at "+pos);
            restored.load(saved); restored.setChanged(); level.sendBlockUpdated(pos,state,state,2); return InteractionResult.FAIL;
        }
        level.levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(state));
        return InteractionResult.SUCCESS;
    }
    private static InteractionResult recoverDoor(UseOnContext context,net.minecraft.core.BlockPos lower) {
        var level=context.getLevel(); var player=context.getPlayer();
        if (player==null || !(level.getBlockEntity(lower) instanceof OwnedDeviceBlockEntity owned)) return InteractionResult.PASS;
        if (tc4revival.config.TCConfig.SERVER.wardedStone.get() && !owned.access().ownedBy(player.getGameProfile().getName())) return InteractionResult.PASS;
        var positions=java.util.List.of(lower,lower.above());
        var states=new java.util.ArrayList<net.minecraft.world.level.block.state.BlockState>();
        var data=new java.util.ArrayList<net.minecraft.nbt.CompoundTag>();
        for (var pos:positions) {
            if (!level.getBlockState(pos).is(TCArcane.ARCANE_DOOR.get()) || !(level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity entity)
                    || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) return InteractionResult.FAIL;
            states.add(level.getBlockState(pos)); data.add(entity.saveWithFullMetadata());
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean complete=false;
        try {
            for (var pos:positions) if (!level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),18)) return InteractionResult.FAIL;
            var drop=new net.minecraft.world.entity.item.ItemEntity(level,lower.getX()+.5,lower.getY()+.5,lower.getZ()+.5,new ItemStack(TCItems.ARCANE_DOOR.get()));
            if (!level.addFreshEntity(drop)) return InteractionResult.FAIL;
            complete=true;
        } finally {
            if (!complete) {
                for (int i=0;i<2;i++) level.setBlock(positions.get(i),states.get(i),18);
                for (int i=0;i<2;i++) {
                    var restored=level.getBlockEntity(positions.get(i));
                    if (restored==null || !level.getBlockState(positions.get(i)).equals(states.get(i))) throw new IllegalStateException("Cannot restore arcane door at "+positions.get(i));
                    restored.load(data.get(i)); restored.setChanged(); level.sendBlockUpdated(positions.get(i),states.get(i),states.get(i),2);
                }
            }
        }
        for (var pos:positions) level.updateNeighborsAt(pos,TCArcane.ARCANE_DOOR.get());
        level.levelEvent(2001,lower,net.minecraft.world.level.block.Block.getId(states.get(0)));
        return InteractionResult.SUCCESS;
    }
}
