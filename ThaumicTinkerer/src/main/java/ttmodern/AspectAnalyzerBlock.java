package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class AspectAnalyzerBlock extends BaseEntityBlock {
    public AspectAnalyzerBlock(){super(Properties.copy(Blocks.OAK_PLANKS).strength(1.7f,1));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AspectAnalyzerEntity(pos,state);}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(player instanceof net.minecraft.server.level.ServerPlayer server&&world.getBlockEntity(pos) instanceof AspectAnalyzerEntity analyzer&&analyzer.usable(player))net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,owner)->new AspectAnalyzerMenu(id,inventory,analyzer),getName()),pos);return InteractionResult.sidedSuccess(world.isClientSide);}
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&!world.isClientSide&&world.getBlockEntity(pos) instanceof AspectAnalyzerEntity analyzer)Containers.dropItemStack(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,analyzer.items.getStackInSlot(0));super.onRemove(state,world,pos,next,moving);}
}
