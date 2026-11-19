package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public  class MetalGrateBlock extends BaseEntityBlock {
    public static final BooleanProperty CLOSED=BooleanProperty.create("closed");
    private static final VoxelShape SHAPE=Block.box(0,13,0,16,16,16);
    public MetalGrateBlock() { super(Properties.of().strength(3,17).sound(SoundType.METAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(CLOSED,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(CLOSED); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new MetalGrateBlockEntity(pos,state); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(context instanceof EntityCollisionContext entity && entity.getEntity() instanceof GrateItemEntity) return Shapes.empty();
        return state.getValue(CLOSED)||context instanceof EntityCollisionContext entity&&entity.getEntity()!=null&&!(entity.getEntity() instanceof ItemEntity)?SHAPE:Shapes.empty();
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide) change(level,pos,state,!state.getValue(CLOSED));return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moving) {
        if(!level.isClientSide) { boolean power=level.hasNeighborSignal(pos);if(power||neighbor.defaultBlockState().isSignalSource()) change(level,pos,state,power); }
    }
    private static void change(Level level,BlockPos pos,BlockState state,boolean closed) {
        if(state.getValue(CLOSED)==closed)return;level.setBlock(pos,state.setValue(CLOSED,closed),2);
        level.playSound(null,pos,closed?net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_CLOSE:net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_OPEN,net.minecraft.sounds.SoundSource.BLOCKS,1,level.random.nextFloat()*.1f+.9f);
    }
}
