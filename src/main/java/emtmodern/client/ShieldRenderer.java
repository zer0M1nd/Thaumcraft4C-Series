package emtmodern.client;

import com.mojang.blaze3d.vertex.*;
import emtmodern.ElectroMagicTools;
import emtmodern.focus.ShieldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The original 12x24 sphere repeats a full texture on every 15-degree quad. */
@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ShieldRenderer extends EntityRenderer<ShieldEntity> {
    protected static final ResourceLocation TEXTURE=ResourceLocation.parse("emt:textures/models/shield.png");
    protected static final float[][] VERTICES=geometry();
    public ShieldRenderer(EntityRendererProvider.Context context){super(context);}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ElectroMagicTools.SHIELD_ENTITY.get(),ShieldRenderer::new);}
    @Override public ResourceLocation getTextureLocation(ShieldEntity shield){return TEXTURE;}
    protected static float[][] geometry(){var vertices=new java.util.ArrayList<float[]>();float hs=-1,hc=0;for(int h=-90;h<90;h+=15){float ns=Mth.sin((float)Math.toRadians(h+15)),nc=Mth.cos((float)Math.toRadians(h+15)),s=0,c=1;for(int a=0;a<360;a+=15){float as=Mth.sin((float)Math.toRadians(a+15)),ac=Mth.cos((float)Math.toRadians(a+15));vertices.add(new float[]{s*hc*2,hs*2,c*hc*2,1,1});vertices.add(new float[]{s*nc*2,ns*2,c*nc*2,1,0});vertices.add(new float[]{as*nc*2,ns*2,ac*nc*2,0,0});vertices.add(new float[]{as*hc*2,hs*2,ac*hc*2,0,1});s=as;c=ac;}hs=ns;hc=nc;}return vertices.toArray(float[][]::new);}
    @Override public void render(ShieldEntity shield,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();if(shield.ownedBy(Minecraft.getInstance().player)){var view=entityRenderDispatcher.camera.getPosition();pose.translate(view.x-Mth.lerp(partial,shield.xOld,shield.getX()),view.y-Mth.lerp(partial,shield.yOld,shield.getY()),view.z-Mth.lerp(partial,shield.zOld,shield.getZ()));}
        var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.sharpTranslucent(TEXTURE));for(var v:VERTICES)out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(1f,1f,1f,1f).uv(v[3],v[4]).uv2(240).endVertex();pose.popPose();
    }
}
