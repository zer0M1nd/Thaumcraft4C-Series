package thmodern.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.TCRenderTypes;
import tc4revival.content.*;
import thmodern.Horizons;
import thmodern.golem.*;

/** The base's marker renderer only recognizes its final item classes; draw addon markers locally. */
@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class HorizonsMarkers {
    private static final ResourceLocation MARK=ResourceLocation.parse("thaumcraft:textures/misc/mark.png");
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var stack=mc.player.getMainHandItem();if(!(stack.getItem() instanceof AnimatedGolemPlacerItem))return;
        if(!stack.hasTag())return;var tag=stack.getTag();
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        for(var entry:tag.getList("markers",Tag.TAG_COMPOUND)){
            var marker=GolemMarker.load((CompoundTag)entry).orElse(null);
            if(marker==null||!marker.dimension().equals(mc.level.dimension().location())||mc.player.distanceToSqr(marker.pos().getCenter())>=4096)continue;
            decal(pose,buffers.getBuffer(TCRenderTypes.runes(MARK)),marker.pos(),marker.face(),marker.color(),.4f);
        }
        pose.popPose();buffers.endBatch(TCRenderTypes.runes(MARK));
    }
    private static void decal(PoseStack pose,VertexConsumer out,BlockPos pos,Direction side,int color,float scale){
        int rgb=color>=0&&color<16?TallowCandleBlock.COLORS[color]:0xffffff;
        pose.pushPose();pose.translate(pos.getX()+.5+side.getStepX()*.51,pos.getY()+.5+side.getStepY()*.51,pos.getZ()+.5+side.getStepZ()*.51);
        pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,0,1),new org.joml.Vector3f(side.getStepX(),side.getStepY(),side.getStepZ())));pose.scale(scale,scale,scale);
        float[][] points={{-.5f,-.5f,0,1},{.5f,-.5f,1,1},{.5f,.5f,1,0},{-.5f,.5f,0,0}};
        for(var v:points)out.vertex(pose.last().pose(),v[0],v[1],0).color((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,.9f).uv(v[2],v[3]).uv2(240).endVertex();
        pose.popPose();
    }
}
