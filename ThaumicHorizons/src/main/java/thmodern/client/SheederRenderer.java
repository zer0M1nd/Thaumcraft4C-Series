package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.creature.Sheeder;
public final class SheederRenderer extends SpiderRenderer<Sheeder> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/entity/sheeder.png"),FLEECE=ResourceLocation.parse("thaumichorizons:textures/entity/sheederoverlay.png");
    public SheederRenderer(EntityRendererProvider.Context context){super(context);layers.clear();
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<Sheeder,net.minecraft.client.model.SpiderModel<Sheeder>>(this){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,Sheeder entity,float swing,float amount,float partial,float age,float yaw,float pitch){
                if(entity.sheared()||entity.isInvisible())return;
                getParentModel().renderToBuffer(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(FLEECE)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(Sheeder entity){return TEXTURE;}
}
