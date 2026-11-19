package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class GolemConnectorBlock extends BaseEntityBlock {
    public static final BooleanProperty CAMOUFLAGED=InterfaceBlock.CAMOUFLAGED;
    public GolemConnectorBlock(){super(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1.7f,1).noOcclusion());registerDefaultState(stateDefinition.any().setValue(CAMOUFLAGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(CAMOUFLAGED);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GolemConnectorEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof GolemConnectorEntity entity))return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);BlockState cover=null;
        if(!stack.isEmpty()){
            if(!(stack.getItem() instanceof BlockItem item) || item.getBlock() instanceof GolemConnectorBlock)return InteractionResult.PASS;
            cover=item.getBlock().defaultBlockState();
            if(cover.hasBlockEntity() || cover.getRenderShape()!=RenderShape.MODEL || !cover.isCollisionShapeFullBlock(level,pos))return InteractionResult.PASS;
            if(cover.hasProperty(RotatedPillarBlock.AXIS))cover=cover.setValue(RotatedPillarBlock.AXIS,hit.getDirection().getAxis());
        }
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!level.isClientSide)entity.cover(cover);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
