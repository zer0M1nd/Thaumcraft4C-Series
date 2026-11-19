package gadomancymodern.client;
import gadomancymodern.node.ManipulationFocusBlock;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
public class ManipulationFocusRenderer implements BlockEntityRenderer<ManipulationFocusBlock.FocusEntity>{
    protected final net.minecraft.client.model.geom.ModelPart random=ManipulationModels.random(),portal=ManipulationModels.portal();
    public ManipulationFocusRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ManipulationFocusBlock.FocusEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(tile.portal(),pose,buffers,light,overlay);}
    public void draw(boolean isPortal,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(.5,-1.375,-.5);var type=RenderType.entityCutoutNoCull(net.minecraft.resources.ResourceLocation.parse("gadomancy:textures/models/manipulation_focus_"+(isPortal?3:0)+".png"));for(int i=0;i<4;i++){(isPortal?portal:random).render(pose,buffers.getBuffer(type),light,overlay);pose.mulPose(Axis.YP.rotationDegrees(90));}pose.popPose();}
}
