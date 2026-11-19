package ttmodern;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class DislocatorBlock extends BaseEntityBlock implements tc4revival.content.WandInteractable {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED,CAMOUFLAGED=InterfaceBlock.CAMOUFLAGED;
    public DislocatorBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,10).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false).setValue(CAMOUFLAGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,POWERED,CAMOUFLAGED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DislocatorEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.DISLOCATOR_ENTITY.get(),(world,pos,s,tile)->tile.tick());}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(level.isClientSide)return;
        boolean power=level.hasNeighborSignal(pos)||level.hasNeighborSignal(pos.above());
        if(power==state.getValue(POWERED))return;
        level.setBlock(pos,state.setValue(POWERED,power),2);
        if(power)level.scheduleTick(pos,this,1);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof DislocatorEntity entity)entity.receivePulse();}
    private InteractionResult orient(Level level,BlockPos pos,Direction face,Player player){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!level.isClientSide){level.setBlock(pos,level.getBlockState(pos).setValue(FACING,face),3);level.playSound(null,pos,tc4revival.content.TCSounds.TOOL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.6f,1);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context){return context.getPlayer()==null?InteractionResult.PASS:orient(context.getLevel(),context.getClickedPos(),context.getClickedFace(),context.getPlayer());}
    @Override public java.util.Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player){return java.util.Optional.empty();}
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining){}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining){}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof DislocatorEntity entity))return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);if(stack.getItem() instanceof tc4revival.content.CastingWandItem)return orient(level,pos,hit.getDirection(),player);
        BlockState cover=null;
        if(!stack.isEmpty()){
            if(!(stack.getItem() instanceof BlockItem item)||item.getBlock() instanceof DislocatorBlock||item.getBlock() instanceof InterfaceBlock)return InteractionResult.PASS;
            cover=item.getBlock().defaultBlockState();if(cover.hasBlockEntity()||cover.getRenderShape()!=RenderShape.MODEL||!cover.isCollisionShapeFullBlock(level,pos))return InteractionResult.PASS;
            if(cover.hasProperty(RotatedPillarBlock.AXIS))cover=cover.setValue(RotatedPillarBlock.AXIS,hit.getDirection().getAxis());
        }
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!level.isClientSide)entity.camouflage(cover);return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
