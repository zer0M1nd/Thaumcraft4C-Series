package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.PrimalOrbEntity;

public  class PrimalOrbRenderer extends EntityRenderer<PrimalOrbEntity> {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final int[] COLORS={0xffff7e,16727041,37119,40960,0xeeccff,0x555577};
    public PrimalOrbRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(PrimalOrbEntity entity) { return ATLAS; }
    @Override public void render(PrimalOrbEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var random=new Random(entity.getId()); var rays=TCRenderTypes.buffer(buffers,TCRenderTypes.PRIMAL_RAYS);
        pose.pushPose();
        for (int i=0;i<12;i++) {
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360)); pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360)); pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360)); pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360+entity.tickCount/80f*360));
            float length=(5+random.nextFloat()*20)*Math.min(entity.tickCount,10)/300f;
            float width=(1+random.nextFloat()*2)*Math.min(entity.tickCount,10)/300f;
            float[][] tips={{-.866f*width,length,-.5f*width},{.866f*width,length,-.5f*width},{0,length,width}};
            int color=COLORS[i/2]; var matrix=pose.last().pose();
            for (int triangle=0;triangle<3;triangle++) {
                rays.vertex(matrix,0,0,0).color(255,255,255,255).endVertex();
                for (int index:new int[]{triangle,(triangle+1)%3}) {
                    var tip=tips[index]; rays.vertex(matrix,tip[0],tip[1],tip[2]).color(color>>16&255,color>>8&255,color&255,0).endVertex();
                }
            }
        }
        pose.popPose(); pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u=entity.tickCount%13/16f;
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.primalSprite(ATLAS)),pose.last().pose(),.25f,u,.125f,u+.0624375f,.1874375f,1,1,1,.8f,240);
        pose.popPose(); super.render(entity,yaw,partial,pose,buffers,light);
    }
}
