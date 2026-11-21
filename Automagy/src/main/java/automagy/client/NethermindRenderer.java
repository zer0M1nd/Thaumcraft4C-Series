package automagy.client;
import automagy.content.NethermindBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
public final class NethermindRenderer implements BlockEntityRenderer<NethermindBlockEntity>{
 public NethermindRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(NethermindBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!tile.enabled())return;var matrix=pose.last();for(boolean top:new boolean[]{true,false}){var out=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.resources.ResourceLocation.parse("automagy:textures/block/nethermind_"+(top?"top":"bottom")+"_glow.png")));float[][] points=top?new float[][]{{0,0},{0,1},{1,1},{1,0}}:new float[][]{{0,0},{1,0},{1,1},{0,1}};for(var p:points)out.vertex(matrix.pose(),p[0],top?1.001f:-.001f,p[1]).color(255,255,255,255).uv(p[0],p[1]).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(),0,top?1:-1,0).endVertex();}}
}
