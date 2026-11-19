package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import thmodern.Horizons;
public final class InfusedWolfRenderer extends WolfRenderer {
    public InfusedWolfRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(Wolf entity){return ResourceLocation.parse("thaumichorizons:textures/entity/"+(entity.getType()==Horizons.SEAWOLF.get()?"seawolf":entity.getType()==Horizons.LUNAR_WOLF.get()?"lunarwolf":"netherhound")+".png");}
    @Override public void render(Wolf wolf,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        super.render(wolf,yaw,partial,pose,buffers,light);
        if(wolf.getType()!=Horizons.LUNAR_WOLF.get()||wolf.isInvisible()||Math.floorMod(wolf.level().getDayTime(),24000)<12000)return;
        float size=wolf.level().getMoonBrightness()*1.5f;if(size<=0)return;
        pose.pushPose();pose.translate(0,wolf.getBbHeight()/1.75,0);pose.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.eldritchEye(ResourceLocation.parse("thaumcraft:textures/misc/nodes.png")));int frame=wolf.tickCount%32;
        for(int i=0;i<4;i++)out.vertex(pose.last().pose(),i==0||i==3?-size:size,i<2?-size:size,0).color(170,221,255,191).uv((frame+(i==0||i==3?0:1))/32f,(i<2?2:1)/32f).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();
        pose.popPose();
    }
}
