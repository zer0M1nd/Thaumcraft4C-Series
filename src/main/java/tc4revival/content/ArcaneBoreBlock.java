package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public  class ArcaneBoreBlock extends BaseEntityBlock implements WandInteractable {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final DirectionProperty MOUNT=DirectionProperty.create("mount");
    private final boolean base;
    public ArcaneBoreBlock(boolean base) {
        super(Properties.of().strength(2.5f,6).sound(SoundType.WOOD).noOcclusion()); this.base=base;
        registerDefaultState(stateDefinition.any().setValue(FACING,base?Direction.NORTH:Direction.UP).setValue(MOUNT,Direction.UP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,MOUNT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing=context.getHorizontalDirection().getOpposite();
        if (!base && context.getPlayer()!=null) {
            var player=context.getPlayer(); var pos=context.getClickedPos();
            if (Math.abs(player.getX()-pos.getX())<2 && Math.abs(player.getZ()-pos.getZ())<2) {
                if (player.getEyeY()-pos.getY()>2) facing=Direction.UP;
                else if (pos.getY()-player.getEyeY()>0) facing=Direction.DOWN;
            }
        }
        var placed=defaultBlockState().setValue(FACING,facing).setValue(MOUNT,context.getClickedFace());
        return canSurvive(placed,context.getLevel(),context.getClickedPos())?placed:null;
    }
    @Override public boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos) {
        if(base)return true;
        var mount=state.getValue(MOUNT);var support=pos.relative(mount.getOpposite());var below=level.getBlockState(support);
        return mount.getAxis()==Direction.Axis.Y && below.is(TCArcane.ARCANE_BORE_BASE.get())
                && below.isFaceSturdy(level,support,mount);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        // A selection hit belongs to this block. Extending it into the next cell
        // produces UseItemOn packets outside vanilla's accepted hit bounds.
        return Shapes.block();
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return base?Shapes.block():super.getBlockSupportShape(state,level,pos); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return base?new ArcaneBoreBaseBlockEntity(pos,state):new ArcaneBoreBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return base?null:createTickerHelper(type,TCArcane.ARCANE_BORE_ENTITY.get(),ArcaneBoreBlockEntity::tick);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos source,boolean moving) {
        if (!base && !level.isClientSide) {
            if (!canSurvive(state,level,pos)) level.destroyBlock(pos,true);
        }
        super.neighborChanged(state,level,pos,block,source,moving);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneBoreBlockEntity bore) Containers.dropContents(level,pos,bore);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (base) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ArcaneBoreBlockEntity bore)
            net.minecraftforge.network.NetworkHooks.openScreen(server,bore,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos();
        if (!level.isClientSide) {
            level.setBlock(pos,level.getBlockState(pos).setValue(FACING,context.getClickedFace()),2);
            if (level.getBlockEntity(pos) instanceof ArcaneBoreBlockEntity bore) bore.work().orient(false);
            level.playSound(null,pos,TCSounds.TOOL.get(),net.minecraft.sounds.SoundSource.BLOCKS,base?.3f:.5f,(base?1.9f:.9f)+level.random.nextFloat()*.2f);
        }
        if (context.getPlayer()!=null) context.getPlayer().swing(context.getHand());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public java.util.Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { return java.util.Optional.empty(); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remainingTicks) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remainingTicks) {}
}
