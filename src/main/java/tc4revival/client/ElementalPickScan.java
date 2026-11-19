package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class ElementalPickScan {
    private record Mark(BlockPos pos,int value,int distanceSquared) {}
    private static final java.util.List<Mark> MARKS=new java.util.ArrayList<>();
    private static Level world;
    private static long expires;
    private ElementalPickScan() {}
    public static void receive(tc4revival.network.ElementalProspectingPacket packet,net.minecraft.network.Connection connection) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.level==null || mc.getConnection()==null || mc.getConnection().getConnection()!=connection || !mc.level.dimension().location().equals(packet.dimension()))return;
        MARKS.clear();world=mc.level;expires=System.currentTimeMillis()+5000;
        for(var mark:packet.marks())MARKS.add(new Mark(packet.center().offset(mark.x(),mark.y(),mark.z()),mark.value(),mark.x()*mark.x()+mark.y()*mark.y()+mark.z()*mark.z()));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc=net.minecraft.client.Minecraft.getInstance(); long now=System.currentTimeMillis(),remaining=expires-now;
        if (world!=mc.level || remaining<=0) { MARKS.clear(); world=null; return; }
        var buffers=mc.renderBuffers().bufferSource(); var camera=event.getCamera(); var view=camera.getPosition();
        var nodes=TCRenderTypes.revealedNode(ResourceLocation.parse("thaumcraft:textures/misc/nodes.png"),false);
        var liquid=TCRenderTypes.revealedNode(ResourceLocation.parse("thaumcraft:textures/blocks/wardedglass.png"),false);
        for (var mark:MARKS) {
            float alpha=remaining>4750?1-(remaining-4750)/250f:remaining<1500?remaining/1500f:1;
            alpha=net.minecraft.util.Mth.clamp(alpha*(1-mark.distanceSquared()/64f),0,1);
            if (alpha<=0) continue;
            var pose=WorldEffectPose.copy(event.getPoseStack()); var pos=mark.pos(); pose.translate(pos.getX()+.5-view.x,pos.getY()+.5-view.y,pos.getZ()+.5-view.z);
            if (mark.value()<0) {
                int color=mark.value()==-5?3986684:16734721;
                for (var direction:Direction.values()) {
                    pose.pushPose();
                    pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/2,-direction.getStepY(),direction.getStepX(),-direction.getStepZ()));
                    pose.translate(0,0,direction.getStepZ()<0?.5:-.5);
                    ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,liquid),pose.last().pose(),.5f,0,1,1,0,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha,200); pose.popPose();
                }
            } else {
                pose.mulPose(camera.rotation()); float frame=(now/50%32)/32f,size=mark.value()/7f;
                ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,nodes),pose.last().pose(),.2f*size,frame+1/32f,0,frame,1/32f,170/255f,170/255f,17/255f,alpha,220);
                ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,nodes),pose.last().pose(),.5f*size,frame+1/32f,0,frame,1/32f,170/255f,17/255f,34/255f,alpha,220);
            }
        }
        TCRenderTypes.endBatch(buffers,nodes); TCRenderTypes.endBatch(buffers,liquid);
    }
}
