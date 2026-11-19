package ttmodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class WarpGateRenderer implements BlockEntityRenderer<WarpGateEntity> {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.WARP_GATE_ENTITY.get(),context->new WarpGateRenderer());}
    private static final int[][] FACES={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
    @Override public void render(WarpGateEntity gate,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var top=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":textures/block/warp_gate_2.png")));
        float[][] corners={{0,0,0,0},{0,1,0,1},{1,1,1,1},{1,0,1,0}};
        for(var v:corners)top.vertex(pose.last().pose(),v[0],1.001f,v[1]).color(255,255,255,255).uv(v[2],v[3]).overlayCoords(overlay).uv2(15728880).normal(pose.last().normal(),0,1,0).endVertex();
        var out=buffers.getBuffer(RenderType.debugQuads());double time=gate.getLevel().getGameTime()+partial;
        for(int trail=0;trail<6;trail++){double ticks=time-.75*trail;for(int i=0;i<12;i++){
            double degrees=(ticks*4+i*30)%360,angle=Math.toRadians(degrees);pose.pushPose();pose.translate(.5+(.7+.1*Math.sin(ticks/6))*Math.cos(angle),1.65-Math.cos((ticks+50*i)/5)*.1,.5+(.7+.1*Math.cos(ticks/6))*Math.sin(angle));
            var axis=new org.joml.Vector3f((float)Math.sin(ticks*.25)/2,(float)Math.max(.6,Math.sin(ticks*.1)/2+.5),(float)Math.cos(ticks*.25)/2).normalize();pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)angle,axis));
            for(var face:FACES)for(int corner:face)out.vertex(pose.last().pose(),(corner&1)/16f,((corner>>1)&1)/16f,((corner>>2)&1)/16f).color(1f,1f,1f,trail==0?1f:.2f).endVertex();pose.popPose();
        }}
    }
}
