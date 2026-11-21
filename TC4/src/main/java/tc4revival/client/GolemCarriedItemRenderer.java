package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import tc4revival.content.*;

public  class GolemCarriedItemRenderer {
    public static void render(GolemEntity golem,PoseStack pose,MultiBufferSource buffers,int light) {
        var core=golem.configuration().core();
        if (core==GolemCoreType.LIQUID || golem.deathTime!=0) return;
        var stack=golem.carried();
        if (stack.isEmpty() && core==GolemCoreType.ESSENTIA) {
            stack=new ItemStack(TCItems.FILLED_ESSENTIA_JAR.get());
            if (golem.essentia()!=null && golem.essentiaAmount()>0) {
                int amount=(int)(64f*golem.essentiaAmount()/golem.configuration().carryLimit());
                tc4revival.aspect.AspectNbt.write(stack.getOrCreateTag(),new tc4revival.aspect.AspectAmounts().add(golem.essentia(),amount));
            }
        }
        if (stack.isEmpty()) return;
        pose.pushPose(); pose.scale(.4f,.4f,.4f);
        var mc=Minecraft.getInstance();
        boolean jar=stack.getItem() instanceof EssentiaJarItem;
        var model=mc.getItemRenderer().getModel(stack,golem.level(),golem,golem.getId());
        if (jar) {
            // Match the wrists of GolemModel: arms at y=30/16, xRot=-1,
            // with the essentia capacity changing zRot. Center the jar, not its bottom.
            double spread=core==GolemCoreType.ESSENTIA?(1-(.5+Math.min(64,golem.configuration().carryLimit())/128d))*Math.toRadians(25):0;
            double wristY=22.5/16d*Math.cos(1),wristZ=-22.5/16d*Math.sin(1);
            double gap=2*(.5*Math.cos(spread)-wristY*Math.sin(spread));
            float scale=(float)(gap/.625);
            pose.translate(0,30/16d+.625*Math.sin(spread)+wristY*Math.cos(spread),wristZ);
            pose.scale(scale,scale,scale);
            pose.mulPose(Axis.XP.rotationDegrees(180)); pose.translate(-.5,-.4375,-.5);
            EssentiaJarRenderer.itemRenderer().renderByItem(stack,ItemDisplayContext.NONE,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        } else {
            boolean block=stack.getItem() instanceof BlockItem && model.isGui3d();
            boolean layered=model.getQuads(null,null,net.minecraft.util.RandomSource.create(0)).stream().anyMatch(quad->quad.getTintIndex()>0);
            pose.translate(block?0:-.5,2.5,-1.25); pose.mulPose(Axis.XP.rotationDegrees(180));
            if (!block) { if (!layered) turnFlatItem(pose); pose.translate(.5,.5,0); }
            mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,OverlayTexture.NO_OVERLAY,pose,buffers,golem.level(),golem.getId());
        }
        pose.popPose();
    }
    private static void turnFlatItem(PoseStack pose) { pose.mulPose(Axis.YP.rotationDegrees(180)); pose.mulPose(Axis.ZN.rotationDegrees(335)); pose.mulPose(Axis.YN.rotationDegrees(50)); }
    private GolemCarriedItemRenderer() {}
}
