package gadomancymodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.aura.AuraPylonEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
public class AuraPylonRenderer implements BlockEntityRenderer<AuraPylonEntity> {
    protected final net.minecraft.client.model.geom.ModelPart middle=AuraPylonModels.middle(),bottom=AuraPylonModels.bottom(),top=AuraPylonModels.top();
    protected static final ResourceLocation MIDDLE=ResourceLocation.parse("gadomancy:textures/models/aura_pylon.png"),BOTTOM=ResourceLocation.parse("gadomancy:textures/models/aura_pylon_base.png"),TOP=ResourceLocation.parse("gadomancy:textures/models/aura_pylon_peak.png");
    public AuraPylonRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(AuraPylonEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){boolean isBottom=tile.index==0||!(tile.getLevel().getBlockEntity(tile.getBlockPos().below()) instanceof AuraPylonEntity);draw(tile.cap(),isBottom,pose,buffers,light,overlay);if(tile.cap())AuraPylonEffects.visible(tile,partial);}
    public void draw(boolean cap,boolean base,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var model=cap?top:base?bottom:middle;var texture=cap?TOP:base?BOTTOM:MIDDLE;pose.pushPose();pose.translate(.5,-.5,.5);for(int i=0;i<4;i++){pose.pushPose();if(cap||base){pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(0,-2,0);}model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,overlay);pose.popPose();pose.mulPose(Axis.YP.rotationDegrees(90));}pose.popPose();}
    @Override public boolean shouldRenderOffScreen(AuraPylonEntity tile){return true;}
}
