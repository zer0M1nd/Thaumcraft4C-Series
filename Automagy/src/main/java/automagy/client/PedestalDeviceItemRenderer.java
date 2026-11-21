package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;

/** Renders the baked stand directly, avoiding recursive custom item rendering. */
public final class PedestalDeviceItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final GolemDeviceRenderer<net.minecraft.world.level.block.entity.BlockEntity> order=new GolemDeviceRenderer<>(false),entropy=new GolemDeviceRenderer<>(true);
    private final RequisitionTomeRenderer book=new RequisitionTomeRenderer(null);
    private final UnseenScribeRenderer scribe=new UnseenScribeRenderer(null);
    public PedestalDeviceItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!(stack.getItem() instanceof PedestalDeviceItem item))return;
        var state=item.getBlock().defaultBlockState();
        if(item.getBlock() instanceof UnseenScribeBlock){
            pose.pushPose();
            // Legacy worldless table: translate 0.07 down and scale 0.98 around the model origin.
            pose.translate(.5,1.43,.5);pose.scale(.98f,.98f,.98f);pose.translate(-.5,-1.5,-.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
            scribe.render(new UnseenScribeBlockEntity(BlockPos.ZERO,state),0,pose,buffers,light,overlay);
            pose.popPose();return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
        if(item.getBlock() instanceof RequisitionTomeBlock)book.render(new RequisitionTomeBlockEntity(BlockPos.ZERO,state),0,pose,buffers,light,overlay);
        else if(item.getBlock() instanceof GolemTaskmasterBlock)order.render(new GolemTaskmasterBlockEntity(BlockPos.ZERO,state),0,pose,buffers,light,overlay);
        else if(item.getBlock() instanceof GolemInhibitorBlock)entropy.render(new GolemInhibitorBlockEntity(BlockPos.ZERO,state),0,pose,buffers,light,overlay);
    }
}
