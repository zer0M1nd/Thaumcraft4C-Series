package tbmodern;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.wands.IWandable;

public final class Relocator extends BlockEntity implements IWandable {
    private int power;
    public Relocator(BlockPos pos,BlockState state) { super(Content.RELOCATOR.get(),pos,state); }
    public static void tick(Level level,BlockPos pos,BlockState state,Relocator tile) {
        Direction direction=state.getValue(Device.FACING);
        if(level.getGameTime()%100==0 || tile.power==0) {
            int count=0;
            // Bounded by loaded chunks; unbounded legacy scans could generate terrain indefinitely.
            for(BlockPos cursor=pos;count<256 && level.hasChunkAt(cursor) && level.getBlockState(cursor).getBlock() instanceof Device;cursor=cursor.relative(direction.getOpposite()))count++;
            tile.power=0;
            for(int distance=1;distance<=count*10;distance++) {
                BlockPos cursor=pos.relative(direction,distance);
                if(level.isOutsideBuildHeight(cursor) || !level.hasChunkAt(cursor) || level.getBlockState(cursor).isSolidRender(level,cursor))break;
                tile.power++;
            }
        }
        if(tile.power==0 || level.hasNeighborSignal(pos))return;
        BlockPos front=pos.relative(direction);
        if(level.getBlockState(front).isFaceSturdy(level,front,direction.getOpposite()))return;
        var bounds=new AABB(pos).expandTowards(direction.getStepX()*tile.power,direction.getStepY()*tile.power,direction.getStepZ()*tile.power);
        boolean attract=((Device)state.getBlock()).attract;
        for(var entity:level.getEntities(null,bounds)) {
            boolean invert=attract != entity.isShiftKeyDown();
            double acceleration=invert?-.1:.1;
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.getStepX()*acceleration,direction.getStepY()*acceleration,direction.getStepZ()*acceleration));
            if(direction.getAxis()==Direction.Axis.Y)entity.fallDistance=0;
            entity.hurtMarked=true;
        }
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        if(!context.getLevel().isClientSide) {
            Direction facing=context.getClickedFace();
            if(context.getPlayer()!=null && context.getPlayer().isShiftKeyDown())facing=facing.getOpposite();
            context.getLevel().setBlock(worldPosition,getBlockState().setValue(Device.FACING,facing),3);power=0;
            context.getLevel().playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.LEVER_CLICK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    public static final class Device extends BaseEntityBlock {
        public static final DirectionProperty FACING=BlockStateProperties.FACING;
        private final boolean attract;
        public Device(boolean attract) {
            super(Properties.of().strength(2.5F,15).sound(SoundType.WOOD));this.attract=attract;
            registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite()); }
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new Relocator(pos,state); }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
            return level.isClientSide?null:createTickerHelper(type,Content.RELOCATOR.get(),Relocator::tick);
        }
        @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
            if(level.hasNeighborSignal(pos))return;
            var direction=state.getValue(FACING);double sign=attract?-1:1;
            double x=pos.getX()+.5+direction.getStepX()*(attract?3:.6),y=pos.getY()+.5+direction.getStepY()*(attract?3:.6),z=pos.getZ()+.5+direction.getStepZ()*(attract?3:.6);
            thaumcraft.api.fx.LegacyEffects.emit(level,"sparkle_path",new net.minecraft.world.phys.Vec3(x,y,z),pos.getCenter().add(direction.getStepX()*(attract?0:3),direction.getStepY()*(attract?0:3),direction.getStepZ()*(attract?0:3)),attract?0x3333dd:0xbaba33,1,3,1,0,0);
        }
    }
}
