package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;
public final class TaintEaterPigRenderer extends PigRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/entity/taintfeeder.png");
    public TaintEaterPigRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<Pig,net.minecraft.client.model.PigModel<Pig>>(this){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,Pig pig,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch){
                if(pig.isInvisible())return;
                float alpha=.32f+.32f*(float)Math.sin(Math.toRadians((pig.tickCount+partial)%45)*8);
                getParentModel().renderToBuffer(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(ResourceLocation.parse("thaumichorizons:textures/entity/taintfeeder_eyes.png"))),net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,alpha);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(Pig pig){return TEXTURE;}
    @Override public void render(Pig pig,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        super.render(pig,yaw,partial,pose,buffers,light);if(pig.isInvisible())return;
        pose.pushPose();pose.translate(0,pig.getBbHeight()/1.75,0);pose.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        int frame=pig.tickCount%32;var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.eldritchEye(ResourceLocation.parse("thaumcraft:textures/misc/nodes.png")));
        for(int i=0;i<4;i++){float x=i==0||i==3?-1:1,y=i<2?-1:1;
            out.vertex(pose.last().pose(),x,y,0).color(170,221,255,255).uv((frame+(i==0||i==3?0:1))/32f,(i<2?7:6)/32f).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();
        }pose.popPose();
    }
}
