package thaumicomniverse.compat.create;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import tc4revival.aspect.Aspect;

/** Identical camera, bellows cycle and depot placement to Create's AnimatedSpout. */
public final class AnimatedEssentiaSpout extends AnimatedKinetics {
    private Aspect aspect;
    public AnimatedEssentiaSpout withAspect(Aspect value){aspect=value;return this;}
    @Override public void draw(GuiGraphics graphics,int x,int y){
        var pose=graphics.pose();pose.pushPose();pose.translate(x,y,100);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5f));pose.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale=20;
        blockElement(ArcaneCreateContent.SPOUT.get().defaultBlockState()).scale(scale).render(graphics);
        float cycle=(AnimationTickHolder.getRenderTime()-offset*8)%30;
        float squeeze=cycle<20?Mth.sin(cycle/20*(float)Math.PI)*20:0;
        pose.pushPose();
        for(var part:ArcaneMachineRenderers.SPOUT){blockElement(part).scale(scale).render(graphics);pose.translate(0,-3*squeeze/32,0);}
        pose.popPose();
        blockElement(AllBlocks.DEPOT.getDefaultState()).atLocal(0,2,0).scale(scale).render(graphics);
        graphics.flush();DEFAULT_LIGHTING.applyLighting();
        if(aspect!=null){
            pose.pushPose();UIRenderHelper.flipForGuiRender(pose);pose.scale(scale,scale,scale);
            ArcaneEssentiaVisuals.liquid(aspect,pose,graphics.bufferSource(),3/16f,3/16f,3/16f,13/16f,13/16f,13/16f);
            // The flow shares the native squeeze envelope and ends at the depot's plate.
            if(squeeze>.01f){float half=squeeze/256;
                ArcaneEssentiaVisuals.liquid(aspect,pose,graphics.bufferSource(),.5f-half,-1.8f,.5f-half,.5f+half,-.25f,.5f+half);
                for(int i=0;i<3;i++){
                    float fall=(cycle/8+i/3f)%1,yDrop=-.3f-1.45f*fall,spread=(i-1)*.07f;
                    ArcaneEssentiaVisuals.liquid(aspect,pose,graphics.bufferSource(),.48f+spread,yDrop,.48f,.52f+spread,yDrop+.04f,.52f);
                }
            }
            graphics.flush();pose.popPose();
        }
        Lighting.setupFor3DItems();pose.popPose();
    }
}
