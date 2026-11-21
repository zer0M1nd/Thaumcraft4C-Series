package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import tc4revival.content.EldritchPortalBlockEntity;

public  class EldritchPortalRenderer implements BlockEntityRenderer<EldritchPortalBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/eldritch_portal.png");
    public EldritchPortalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 96; }
    @Override public void render(EldritchPortalBlockEntity portal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        thaumcraft.api.client.DeviceRendering.render(new thaumcraft.api.client.DeviceRendering.Context<>(portal,partial,pose,buffers,light,overlay),()->renderDefault(portal,partial,pose,buffers,light,overlay));
    }
    private void renderDefault(EldritchPortalBlockEntity portal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (!(Minecraft.getInstance().getCameraEntity() instanceof net.minecraft.world.entity.player.Player)) return;
        float width=(int)Math.min(5,portal.opening()+partial)/5f,height=(int)Math.min(30,portal.opening()+partial)/30f;
        int frame=(int)((System.nanoTime()/50_000_000L)%16); float u=frame/16f;
        var orientation=Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var out=TCRenderTypes.buffer(buffers,TCRenderTypes.sharpTranslucent(TEXTURE)); var matrix=pose.last().pose();
        float[][] corners={{-1,-1,u,1},{-1,1,u+.0625f,1},{1,1,u+.0625f,0},{1,-1,u,0}};
        for (float[] corner:corners) {
            var point=new Vector3f(corner[0],corner[1],0).rotate(orientation);
            out.vertex(matrix,.5f+point.x*width,.5f+point.y*height,.5f+point.z*width).color(1f,1f,1f,1f).uv(corner[2],corner[3]).uv2(220).endVertex();
        }
    }
}
