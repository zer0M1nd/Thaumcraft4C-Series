package automagy.client;
import automagy.content.DimensionLureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
public final class DimensionLureRenderer implements BlockEntityRenderer<DimensionLureBlockEntity>{
 public DimensionLureRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(DimensionLureBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!tile.enabled())return;double time=tile.getLevel()==null?0:tile.getLevel().getGameTime()+partial;float alpha=(float)(.75+.25*Math.cos(time*Math.PI/50));renderGlow(pose,buffers,overlay,alpha);}
 public static void renderGlow(PoseStack pose,MultiBufferSource buffers,int overlay,float alpha){var out=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.resources.ResourceLocation.parse("automagy:textures/block/dimension_lure_overlay.png")));var matrix=pose.last();float[][] points={{0,0,0,0},{0,1,0,1},{1,1,1,1},{1,0,1,0}};for(var p:points)out.vertex(matrix.pose(),p[0],1.001f,p[1]).color(1f,1f,1f,alpha).uv(p[2],p[3]).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(),0,1,0).endVertex();}
}
