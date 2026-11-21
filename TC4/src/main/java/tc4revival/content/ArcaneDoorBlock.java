package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Uses modern door geometry while retaining TC4's ownership and plate-only activation. */
public  class ArcaneDoorBlock extends DoorBlock implements EntityBlock {
    public ArcaneDoorBlock() { super(Properties.of().strength(15,999).sound(SoundType.METAL).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK),BlockSetType.IRON); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new OwnedDeviceBlockEntity(pos,state); }
    public static BlockPos root(BlockState state,BlockPos pos) { return state.getValue(HALF)==DoubleBlockHalf.UPPER ? pos.below() : pos; }
    @Override public float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos pos) {
        return tc4revival.config.TCConfig.SERVER.wardedStone.get() ? 0 : super.getDestroyProgress(state,player,level,pos);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack) {}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(root(state,pos)) instanceof OwnedDeviceBlockEntity owned) {
            if (owned.access().mayUse(player.getGameProfile().getName())) changeOpen(level,root(state,pos),!state.getValue(OPEN));
            else {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("The door refuses to budge."),false);
                level.playSound(null,pos,TCSounds.DOOR_FAIL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.66f,1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    // Prevent vanilla door AI from bypassing permissions through setOpen.
    @Override public void setOpen(net.minecraft.world.entity.Entity entity,Level level,BlockState state,BlockPos pos,boolean open) {}
    static void changeOpen(Level level,BlockPos lower,boolean open) {
        var state=level.getBlockState(lower); var upper=level.getBlockState(lower.above());
        if (!(state.getBlock() instanceof ArcaneDoorBlock) || !upper.is(state.getBlock()) || state.getValue(OPEN)==open) return;
        level.setBlock(lower,state.setValue(OPEN,open),2);
        level.setBlock(lower.above(),upper.setValue(OPEN,open),2);
        level.playSound(null,lower,level.random.nextBoolean() ? net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS,1,.9f+level.random.nextFloat()*.1f);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos sourcePos,boolean moving) {
        if (level.isClientSide) return;
        var lower=root(state,pos);
        if (source==TCArcane.ARCANE_PRESSURE_PLATE.get()) {
            if (!(level.getBlockEntity(lower) instanceof OwnedDeviceBlockEntity door)) return;
            int signal=0;
            for (var direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
                var platePos=pos.relative(direction); var plate=level.getBlockState(platePos);
                if (plate.is(TCArcane.ARCANE_PRESSURE_PLATE.get()) && level.getBlockEntity(platePos) instanceof OwnedDeviceBlockEntity owned
                        && door.access().matchesPressurePlate(owned.access())) {
                    signal=plate.getValue(ArcanePressurePlateBlock.POWERED) ? 1 : -1;
                    if (signal==1) break;
                }
            }
            if (signal!=0) changeOpen(level,lower,signal==1);
        } else {
            var other=state.getValue(HALF)==DoubleBlockHalf.UPPER ? pos.below() : pos.above();
            if (!level.getBlockState(other).is(this)) level.destroyBlock(pos,true);
            else if (state.getValue(HALF)==DoubleBlockHalf.UPPER && source!=this && source!=Blocks.AIR)
                neighborChanged(level.getBlockState(lower),level,lower,source,sourcePos,moving);
        }
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.entity.Entity entity) { return false; }
    @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) { return false; }
    @Override public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion) {}

}
