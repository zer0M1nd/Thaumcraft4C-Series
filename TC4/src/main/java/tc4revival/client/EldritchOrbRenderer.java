package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EldritchOrbEntity;

public  class EldritchOrbRenderer extends EntityRenderer<EldritchOrbEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public EldritchOrbRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(EldritchOrbEntity entity) { return TEXTURE; }
    @Override public void render(EldritchOrbEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var random=new java.util.Random(entity.getId()); var vertices=TCRenderTypes.buffer(buffers,TCRenderTypes.PRIMAL_RAYS);
        pose.pushPose();
        for (int ray=0;ray<12;ray++) {
            for (int rotation=0;rotation<6;rotation++) {
                float angle=random.nextFloat()*360+(rotation==5 ? entity.tickCount/80f*360 : 0);
                pose.mulPose((rotation%3==0 ? Axis.XP : rotation%3==1 ? Axis.YP : Axis.ZP).rotationDegrees(angle));
            }
            float growth=Math.min(entity.tickCount,10)/300f;
            float length=(random.nextFloat()*20+5)*growth,width=(random.nextFloat()*2+1)*growth;
            float[][] rim={{-.866f*width,length,-.5f*width},{.866f*width,length,-.5f*width},{0,length,width}};
            for (int face=0;face<3;face++) {
                vertices.vertex(pose.last().pose(),0,0,0).color(255,255,255,255).endVertex();
                for (int index:new int[]{face,(face+1)%3}) {
                    float[] point=rim[index]; vertices.vertex(pose.last().pose(),point[0],point[1],point[2]).color(85,85,119,0).endVertex();
                }
            }
        }
        pose.popPose(); pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u=entity.tickCount%13/16f;
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.orb(TEXTURE)),pose.last().pose(),.5f,u,.1875f,u+.0624375f,.2499375f,1,1,1,1,240);
        pose.popPose(); super.render(entity,yaw,partial,pose,buffers,light);
    }
}
