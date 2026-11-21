package gadomancymodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.compressor.EssentiaCompressorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
public class EssentiaCompressorRenderer implements BlockEntityRenderer<EssentiaCompressorEntity> {
    protected final net.minecraft.client.model.geom.ModelPart formed=CompressorModels.formed(),packed=CompressorModels.packed();
    protected static final ResourceLocation FORMED=ResourceLocation.parse("gadomancy:textures/models/essentia_compressor.png"),PACKED=ResourceLocation.parse("gadomancy:textures/blocks/block_packed_compressor.png"),VORTEX=ResourceLocation.parse("thaumcraft:textures/misc/vortex.png");
    public EssentiaCompressorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(EssentiaCompressorEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!tile.formed()){packed(pose,buffers,light,overlay,false);return;}if(tile.index!=0)return;pose.pushPose();pose.translate(.5,-.5,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(0,-2,0);var out=buffers.getBuffer(RenderType.entityCutoutNoCull(FORMED));formed.render(pose,out,light,overlay);pose.mulPose(Axis.XP.rotationDegrees(180));formed.render(pose,out,light,overlay);pose.popPose();vortex(tile,partial,pose,buffers);}
    public void packed(PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean item){pose.pushPose();pose.translate(.5,-.5,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(0,item?-2.9:-2.877,0);packed.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(PACKED)),light,overlay);pose.popPose();}
    protected void vortex(EssentiaCompressorEntity tile,float partial,PoseStack pose,MultiBufferSource buffers){var mc=Minecraft.getInstance();long time=tile.getLevel().getGameTime();float age=(time%800)/400f;if(age>=1)age=2-age;float half=(.2f+.1f*age)*(1+tile.stage*.04f);pose.pushPose();pose.translate(.5,1.5,.5);if(tile.stage>4){float shake=.001f*(tile.stage-4);var r=tile.getLevel().random;pose.translate((r.nextFloat()*2-1)*shake,(r.nextFloat()*2-1)*shake,(r.nextFloat()*2-1)*shake);}pose.mulPose(mc.gameRenderer.getMainCamera().rotation());pose.mulPose(Axis.ZP.rotation((float)(Math.PI*2*(1-(time%300)/300f))));var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.sharpTranslucent(VORTEX));var matrix=pose.last().pose();out.vertex(matrix,-half,-half,0).color(255,255,255,255).uv(0,1).uv2(220).endVertex();out.vertex(matrix,-half,half,0).color(255,255,255,255).uv(1,1).uv2(220).endVertex();out.vertex(matrix,half,half,0).color(255,255,255,255).uv(1,0).uv2(220).endVertex();out.vertex(matrix,half,-half,0).color(255,255,255,255).uv(0,0).uv2(220).endVertex();pose.popPose();}
    @Override public boolean shouldRenderOffScreen(EssentiaCompressorEntity tile){return true;}
}
