package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.GolemBobberEntity;

public  class GolemBobberRenderer extends EntityRenderer<GolemBobberEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("minecraft:textures/entity/fishing_hook.png");
    public GolemBobberRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(GolemBobberEntity entity) { return TEXTURE; }
    @Override public void render(GolemBobberEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.sharpTranslucent(TEXTURE)),pose.last().pose(),.25f,
                0,0,1,1,1,1,1,1,light);
        pose.popPose();
        var fisher=entity.fisher();
        if (fisher!=null) {
            float facing=Mth.lerp(partial,fisher.yBodyRotO,fisher.yBodyRot)*Mth.DEG_TO_RAD;
            double sin=Mth.sin(facing),cos=Mth.cos(facing);
            double handX=Mth.lerp(partial,fisher.xo,fisher.getX())-cos*.25-sin*.7;
            double handY=Mth.lerp(partial,fisher.yo,fisher.getY())+fisher.getEyeHeight()-.4;
            double handZ=Mth.lerp(partial,fisher.zo,fisher.getZ())-sin*.25+cos*.7;
            float dx=(float)(handX-Mth.lerp(partial,entity.xo,entity.getX()));
            float dy=(float)(handY-Mth.lerp(partial,entity.yo,entity.getY())-.25);
            float dz=(float)(handZ-Mth.lerp(partial,entity.zo,entity.getZ()));
            var vertices=TCRenderTypes.buffer(buffers,RenderType.debugLineStrip(1));
            for (int index=0;index<=16;index++) {
                float fraction=index/16f;
                vertices.vertex(pose.last().pose(),dx*fraction,dy*(fraction*fraction+fraction)*.5f+.25f,dz*fraction).color(0,0,0,255).endVertex();
            }
        }
        super.render(entity,yaw,partial,pose,buffers,light);
    }
}
