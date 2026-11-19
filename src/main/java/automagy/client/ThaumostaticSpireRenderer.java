package automagy.client;

import automagy.content.ThaumostaticSpireBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

/** The shell uses the generated block model; this billboard is the legacy refueling ring. */
public final class ThaumostaticSpireRenderer implements BlockEntityRenderer<ThaumostaticSpireBlockEntity> {
    private static final ResourceLocation RING=ResourceLocation.parse("thaumcraft:textures/item/lightningring.png");
    public ThaumostaticSpireRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ThaumostaticSpireBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!tile.refueling())return;
        pose.pushPose();pose.translate(.5,2.05,.5);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        var matrix=pose.last();var out=buffers.getBuffer(RenderType.entityTranslucent(RING));
        float[][] vertices={{-1.25f,-1.25f,0,1},{1.25f,-1.25f,1,1},{1.25f,1.25f,1,0},{-1.25f,1.25f,0,0}};
        for(var vertex:vertices)out.vertex(matrix.pose(),vertex[0],vertex[1],0).color(255,255,255,230).uv(vertex[2],vertex[3]).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(),0,0,1).endVertex();
        pose.popPose();
    }
}
