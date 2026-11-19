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

public final class InterfaceBlock extends BaseEntityBlock {
    public static final BooleanProperty CAMOUFLAGED=BooleanProperty.create("camouflaged");
    public InterfaceBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,10).noOcclusion());registerDefaultState(stateDefinition.any().setValue(CAMOUFLAGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(CAMOUFLAGED);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InterfaceEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.INTERFACE_ENTITY.get(),(world,pos,current,entity)->entity.refreshDestination());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof InterfaceEntity entity))return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);BlockState cover=null;
        if(!stack.isEmpty()){
            if(!(stack.getItem() instanceof BlockItem item) || item.getBlock() instanceof InterfaceBlock)return InteractionResult.PASS;
            cover=item.getBlock().defaultBlockState();
            if(cover.hasBlockEntity() || cover.getRenderShape()!=RenderShape.MODEL || !cover.isCollisionShapeFullBlock(level,pos))return InteractionResult.PASS;
            if(cover.hasProperty(RotatedPillarBlock.AXIS))cover=cover.setValue(RotatedPillarBlock.AXIS,hit.getDirection().getAxis());
        }
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!level.isClientSide)entity.camouflage(cover);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
