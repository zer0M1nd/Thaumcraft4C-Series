package emtmodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import emtmodern.ElectroMagicTools;
import emtmodern.focus.LaserEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class LaserRenderer extends EntityRenderer<LaserEntity> {
    protected static final ResourceLocation TEXTURE=ResourceLocation.parse("emt:textures/models/lasermodel.png");
    public LaserRenderer(EntityRendererProvider.Context context){super(context);}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ElectroMagicTools.LASER_ENTITY.get(),LaserRenderer::new);}
    @Override public ResourceLocation getTextureLocation(LaserEntity entity){return TEXTURE;}
    @Override public void render(LaserEntity laser,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(laser.getYRot()-90));pose.mulPose(Axis.ZP.rotationDegrees(laser.getXRot()));pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(.05625f,.05625f,.05625f);pose.translate(-4,0,0);var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.sharpTranslucent(TEXTURE));
        for(int i=0;i<4;i++){pose.mulPose(Axis.XP.rotationDegrees(90));var m=pose.last().pose();float[][] v={{-8,0,.4f,0},{8,0,.7f,0},{8,4,.7f,5/32f},{-8,4,.4f,5/32f},{8,0,.4f,0},{-8,0,.7f,0},{-8,4,.7f,5/32f},{8,4,.4f,5/32f}};for(var p:v)out.vertex(m,p[0],p[1],0).color(1f,1f,1f,1f).uv(p[2],p[3]).uv2(240).endVertex();}pose.popPose();
    }
}
