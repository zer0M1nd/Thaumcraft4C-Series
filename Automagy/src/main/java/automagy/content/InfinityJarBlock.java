package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public final class InfinityJarBlock extends BaseEntityBlock {
 public InfinityJarBlock(){super(Properties.of().strength(.3f).sound(SoundType.GLASS).noOcclusion());}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfinityJarBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.INFINITY_JAR_TILE.get(),InfinityJarBlockEntity::tick);}
 @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return Block.box(3,0,3,13,12,13);}
 @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
    var stack=player.getItemInHand(hand);if(!(stack.getItem() instanceof tc4revival.content.StoredAspectItem source))return net.minecraft.world.InteractionResult.PASS;
    var values=source.storedAspects(stack);if(values.isEmpty())return net.minecraft.world.InteractionResult.PASS;
    if(!level.isClientSide&&level.getBlockEntity(pos) instanceof InfinityJarBlockEntity tile)tile.select(values.aspects().iterator().next());
    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){automagy.client.InfinityJarEffects.animate(level,pos,random);}
}
