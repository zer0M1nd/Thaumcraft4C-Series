package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PlatformBlock extends BaseEntityBlock {
    public PlatformBlock() { super(Properties.copy(Blocks.OAK_PLANKS).strength(2,5).noOcclusion().dynamicShape());registerDefaultState(stateDefinition.any().setValue(InterfaceBlock.CAMOUFLAGED,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(InterfaceBlock.CAMOUFLAGED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new PlatformEntity(pos,state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        if(!(context instanceof EntityCollisionContext collision)||collision.getEntity()==null)return Shapes.empty();
        var entity=collision.getEntity();
        // Modern player coordinates are feet positions; the old client used eye-height coordinates.
        return entity.getY()>=pos.getY()+1-.00001&&(!(entity instanceof Player)||!entity.isShiftKeyDown())?Shapes.block():Shapes.empty();
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof PlatformEntity entity))return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);BlockState cover=null;
        if(!stack.isEmpty()) {
            if(!(stack.getItem() instanceof BlockItem item)||item.getBlock() instanceof PlatformBlock||item.getBlock() instanceof InterfaceBlock)return InteractionResult.PASS;
            cover=item.getBlock().defaultBlockState();
            if(cover.hasBlockEntity()||!cover.isCollisionShapeFullBlock(level,pos)||cover.getRenderShape()!=RenderShape.MODEL)return InteractionResult.PASS;
            if(cover.hasProperty(RotatedPillarBlock.AXIS))cover=cover.setValue(RotatedPillarBlock.AXIS,hit.getDirection().getAxis());
        }
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResult.FAIL;
        if(!level.isClientSide)entity.cover(cover);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
