package advancedthaumaturgy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public class WandbenchBlock extends BaseEntityBlock {
    public WandbenchBlock(){super(Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WandbenchEntity(p,s);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){
        if(!l.mayInteract(player,p)||!player.mayUseItemAt(p,hit.getDirection(),player.getItemInHand(h))||!ATConfig.enabled("wandbench"))return InteractionResult.FAIL;
        if(player instanceof net.minecraft.server.level.ServerPlayer sp&&l.getBlockEntity(p) instanceof WandbenchEntity bench)
            net.minecraftforge.network.NetworkHooks.openScreen(sp,new SimpleMenuProvider((id,inv,pl)->new WandbenchMenu(id,inv,bench),net.minecraft.network.chat.Component.translatable("block.advancedthaumaturgy.wandbench")),p);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof WandbenchEntity bench)Containers.dropContents(l,p,bench);super.onRemove(s,l,p,next,moving);}
}
