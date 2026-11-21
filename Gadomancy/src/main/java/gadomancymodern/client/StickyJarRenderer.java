package gadomancymodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.automation.StickyJarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;

public class StickyJarRenderer implements BlockEntityRenderer<StickyJarBlockEntity> {
    protected final tc4revival.client.EssentiaJarRenderer renderer=new tc4revival.client.EssentiaJarRenderer(null);
    public StickyJarRenderer(BlockEntityRendererProvider.Context context){}
    public static void rotate(PoseStack pose,Direction support,Direction facing){
        if(support==Direction.UP){pose.translate(Math.abs(facing.getStepZ()),1,Math.abs(facing.getStepX()));pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI,facing.getStepX(),0,facing.getStepZ()));}
        else if(support!=Direction.DOWN){pose.translate(.5,.5,.5);pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/2,-support.getStepZ(),0,support.getStepX()));pose.mulPose(Axis.YP.rotationDegrees(switch(support){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));pose.translate(-.5,-.5,-.5);}
    }
    @Override public void render(StickyJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();rotate(pose,jar.support(),jar.facing());renderer.render(jar,partial,pose,buffers,light,overlay);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock((jar.voidJar()?tc4revival.content.TCArcane.VOID_JAR.get():tc4revival.content.TCArcane.ESSENTIA_JAR.get()).defaultBlockState(),pose,buffers,light,overlay);pose.popPose();
    }
}
