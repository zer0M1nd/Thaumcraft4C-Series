package gadomancymodern.client;
import gadomancymodern.maze.AdditionalPortalEntity;
import net.minecraft.client.renderer.blockentity.*;
public class AdditionalPortalRenderer implements BlockEntityRenderer<AdditionalPortalEntity>{
    public AdditionalPortalRenderer(BlockEntityRendererProvider.Context context){}
    @Override public int getViewDistance(){return 96;}
    @Override public void render(AdditionalPortalEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){if(!(net.minecraft.client.Minecraft.getInstance().getCameraEntity() instanceof net.minecraft.world.entity.player.Player))return;float width=(int)Math.min(5,tile.opening()+partial)/5f,height=(int)Math.min(30,tile.opening()+partial)/30f;int frame=(int)((System.nanoTime()/50_000_000L)%16);float u=frame/16f;var camera=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.sharpTranslucent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/misc/eldritch_portal.png")));
        for(float[] corner:new float[][]{{-1,-1,u,1},{-1,1,u+.0625f,1},{1,1,u+.0625f,0},{1,-1,u,0}}){var point=new org.joml.Vector3f(corner[0],corner[1],0).rotate(camera);out.vertex(pose.last().pose(),.5f+point.x*width,.5f+point.y*height,.5f+point.z*width).color(1f,1f,1f,1f).uv(corner[2],corner[3]).uv2(220).endVertex();}}
}
