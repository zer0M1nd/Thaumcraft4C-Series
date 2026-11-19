package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ConciliumCrucibleRenderer implements BlockEntityRenderer<ConciliumCrucibleBlockEntity> {
    private static final ResourceLocation QUICKSILVER=ResourceLocation.parse("thaumicconcilium:textures/block/quicksilver.png"),FLESH=ResourceLocation.parse("thaumicconcilium:textures/block/liquid_flesh.png");
    public ConciliumCrucibleRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ConciliumCrucibleBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.amount()<=0)return;
        var out=buffers.getBuffer(RenderType.entityTranslucent(tile.flesh()?FLESH:QUICKSILVER));
        for(float[] p:new float[][]{{0,0},{0,1},{1,1},{1,0}})out.vertex(pose.last().pose(),p[0],tile.fluidHeight(),p[1])
            .color(255,255,255,255).uv(p[0],p[1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,1,0).endVertex();
    }
}
