package thmodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
public final class SpikeItemRenderer extends BlockEntityWithoutLevelRenderer {
 public SpikeItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!(stack.getItem() instanceof BlockItem item))return;pose.pushPose();
  if(context==ItemDisplayContext.GUI){pose.translate(.5,.5,.5);pose.mulPose(Axis.XP.rotationDegrees(30));pose.mulPose(Axis.YP.rotationDegrees(45));pose.scale(.65f,.65f,.65f);pose.translate(-.5,-.5,-.5);}
  SpikeRenderer.render(item.getBlock().defaultBlockState(),pose,buffers,light,overlay);pose.popPose();
 }
}
