package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public  class EldritchLockBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public EldritchLockBlock() {
        super(Properties.of().strength(-1,Float.MAX_VALUE).noOcclusion().sound(SoundType.STONE).lightLevel(state->5).isValidSpawn((state,world,pos,type)->false));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof EldritchLockBlockEntity lock && lock.count()>=0)
            tc4revival.client.EldritchSparkParticles.emit(level,pos.getX()+random.nextFloat(),pos.getY()+random.nextFloat(),pos.getZ()+random.nextFloat(),.5f,.65f+random.nextFloat()*.1f,1,1,.8f);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EldritchLockBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCEldritch.LOCK_ENTITY.get(),(world,pos,current,lock)->lock.tick());
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (player.getItemInHand(hand).is(TCEldritch.RUNED_TABLET.get()) && level.getBlockEntity(pos) instanceof EldritchLockBlockEntity lock && lock.count()<0) {
            if (!level.isClientSide && lock.activate()) player.getItemInHand(hand).shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
