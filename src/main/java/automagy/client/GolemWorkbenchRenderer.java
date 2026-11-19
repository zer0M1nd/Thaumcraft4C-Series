package automagy.client;
import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
public final class GolemWorkbenchRenderer implements BlockEntityRenderer<GolemWorkbenchBlockEntity>{
 public GolemWorkbenchRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(GolemWorkbenchBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  for(int socket=0;socket<4;socket++){var item=Content.WORKBENCH_UPGRADES.get(tile.upgrade(socket));if(item==null)continue;var face=Direction.from3DDataValue(socket+2);pose.pushPose();pose.translate(.5+face.getStepX()*.502,.375,.5+face.getStepZ()*.502);pose.mulPose(Axis.YP.rotationDegrees(180-face.toYRot()));pose.scale(.375f,.375f,.375f);Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(item.get()),ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),socket);pose.popPose();}
 }
}
