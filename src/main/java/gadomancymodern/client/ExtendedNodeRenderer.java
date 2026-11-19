package gadomancymodern.client;
import gadomancymodern.node.ExtendedNodeEntity;
import net.minecraft.client.renderer.blockentity.*;
public class ExtendedNodeRenderer implements BlockEntityRenderer<ExtendedNodeEntity> {
    protected final tc4revival.client.AuraNodeRenderer renderer;
    public ExtendedNodeRenderer(BlockEntityRendererProvider.Context context){renderer=new tc4revival.client.AuraNodeRenderer(context);}
    @Override public void render(ExtendedNodeEntity node,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){pose.pushPose();if(node.growing()){pose.translate(.5,.5,.5);pose.scale(1.5f,1.5f,1.5f);pose.translate(-.5,-.5,-.5);}renderer.render(node,partial,pose,buffers,light,overlay);pose.popPose();}
}
