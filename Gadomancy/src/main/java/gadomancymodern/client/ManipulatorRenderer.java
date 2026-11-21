package gadomancymodern.client;
import gadomancymodern.node.*;
import net.minecraft.client.renderer.blockentity.*;
import com.mojang.math.Axis;
public class ManipulatorRenderer implements BlockEntityRenderer<NodeManipulatorEntity> {
    public ManipulatorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(NodeManipulatorEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){if(tile.isEmpty())return;var mc=net.minecraft.client.Minecraft.getInstance();var viewer=mc.getCameraEntity();float ticks=(viewer==null?0:viewer.tickCount)+partial;pose.pushPose();pose.translate(.5,1.25+Math.sin(ticks%32767/16)*.05,.5);pose.mulPose(Axis.YP.rotationDegrees(ticks%360));mc.getItemRenderer().renderStatic(tile.getItem(0),net.minecraft.world.item.ItemDisplayContext.GROUND,light,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();}
}
