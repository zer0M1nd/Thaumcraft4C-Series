package thaumicfutures.foci.client;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import tc4revival.client.TCRenderTypes;
import tc4revival.content.*;
import thaumicfutures.foci.*;

/** Original TC5 sprite frames, purple ribbon trail and animated elemental tether. */
@Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT)
public final class FocusVisuals {
    public static final ResourceLocation ATLAS=ResourceLocation.parse("thaumicfutures:textures/misc/tc5_particles.png");
    private static final ResourceLocation BEAM=ResourceLocation.parse("thaumicfutures:textures/misc/tc5_beaml.png");
    private record Burst(Vec3 position,long start) {}
    private static final List<Burst> BURSTS=new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel burstLevel;
    private FocusVisuals() {}
    public static void burst(Entity entity) {
        var mc=Minecraft.getInstance();if(entity.level()!=mc.level)return;
        if(burstLevel!=mc.level) { BURSTS.clear();burstLevel=mc.level; }
        if(BURSTS.size()<512)BURSTS.add(new Burst(entity.position(),mc.level.getGameTime()));
    }
    @Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(FutureFoci.SHARD_ENTITY.get(),ShardRenderer::new);
            event.registerEntityRenderer(FutureFoci.GRAPPLE_ENTITY.get(),GrappleRenderer::new);
        }
    }
    private static void vertex(VertexConsumer out,Matrix4f matrix,Vec3 p,float u,float v,float r,float g,float b,float a) {
        out.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).color(r,g,b,a).uv(u,v).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }
    private static void quad(VertexConsumer out,Matrix4f matrix,float size,float u,float v,float cell,float r,float g,float b,float alpha) {
        vertex(out,matrix,new Vec3(-size,-size,0),u,v+cell,r,g,b,alpha);
        vertex(out,matrix,new Vec3(size,-size,0),u+cell,v+cell,r,g,b,alpha);
        vertex(out,matrix,new Vec3(size,size,0),u+cell,v,r,g,b,alpha);
        vertex(out,matrix,new Vec3(-size,size,0),u,v,r,g,b,alpha);
    }
    public static class ShardRenderer extends EntityRenderer<HomingShardEntity> {
        public ShardRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(HomingShardEntity entity) { return ATLAS; }
        @Override public void render(HomingShardEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
            quad(buffers.getBuffer(TCRenderTypes.softParticle(ATLAS)),pose.last().pose(),(.4f+.1f*entity.strength())/2,
                    (8+entity.tickCount%8)/16f,.25f,1/16f,.405f,.075f,.525f,1);
            pose.popPose();
            var origin=entity.getPosition(partial);var camera=entityRenderDispatcher.camera.getPosition().subtract(origin);
            var out=buffers.getBuffer(TCRenderTypes.runes(BEAM));var matrix=pose.last().pose();var from=Vec3.ZERO;
            int count=entity.trail.size(),index=0;
            for(var history:entity.trail) {
                var to=history.subtract(origin);var segment=to.subtract(from);
                var side=segment.cross(camera.subtract(from)).normalize();
                double width=.25*(count-index)/Math.max(1.,count),next=.25*(count-index-1)/Math.max(1.,count);
                vertex(out,matrix,from.add(side.scale(width)),0,0,.405f,.075f,.525f,1);
                vertex(out,matrix,from.subtract(side.scale(width)),0,1,.405f,.075f,.525f,1);
                vertex(out,matrix,to.subtract(side.scale(next)),1,1,.405f,.075f,.525f,1);
                vertex(out,matrix,to.add(side.scale(next)),1,0,.405f,.075f,.525f,1);
                from=to;index++;
            }
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    public static class GrappleRenderer extends EntityRenderer<GrappleEntity> {
        public GrappleRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(GrappleEntity entity) { return ATLAS; }
        @Override public boolean shouldRender(GrappleEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z) {
            return entity.getOwner()!=null || super.shouldRender(entity,frustum,x,y,z);
        }
        @Override public void render(GrappleEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            var out=buffers.getBuffer(TCRenderTypes.runes(ATLAS));
            pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float size=(1+(float)Math.sin(entity.tickCount/5f)*.2f+.2f)/2;
            quad(out,pose.last().pose(),size,(1+entity.tickCount%6)/8f,.875f,.125f,entity.sticky()?0:1,1,entity.sticky()?.2f:1,1);
            pose.popPose();var owner=entity.getOwner();
            if(owner!=null) {
                var delta=owner.getPosition(partial).add(0,owner.getBbHeight()/2,0).subtract(entity.getPosition(partial));
                int steps=Math.max(2,Math.min(1024,(int)(delta.length()*5)));
                for(int i=0;i<=steps;i++) {
                    double d=(double)i/steps;var point=delta.scale(d);
                    if(i>0 && i<steps)point=point.add(Math.sin(i/10f)*entity.amplitude,Math.sin(i/8f)*entity.amplitude,Math.sin(i/6f)*entity.amplitude);
                    pose.pushPose();pose.translate(point.x,point.y,point.z);pose.mulPose(entityRenderDispatcher.cameraOrientation());
                    quad(out,pose.last().pose(),.075f,(entity.tickCount+i)%13/16f,2/16f,1/16f,1,1,1,1);pose.popPose();
                }
            }
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    @SubscribeEvent public static void renderWorld(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null)return;
        if(burstLevel!=mc.level) { BURSTS.clear();burstLevel=mc.level; }
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();var camera=event.getCamera().getPosition();
        BURSTS.removeIf(b -> mc.level.getGameTime()-b.start>12);
        for(var burst:BURSTS) {
            float age=mc.level.getGameTime()-burst.start+event.getPartialTick();var p=burst.position.subtract(camera);
            pose.pushPose();pose.translate(p.x,p.y,p.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            quad(buffers.getBuffer(TCRenderTypes.runes(ATLAS)),pose.last().pose(),.15f+age*.025f,(8+(int)age%8)/16f,.25f,1/16f,.7f,.3f,1,Math.max(0,1-age/12));pose.popPose();
        }
        if(mc.screen==null)for(var hand:InteractionHand.values()) {
            var wand=mc.player.getItemInHand(hand);
            if(!(wand.getItem() instanceof CastingWandItem casting) || casting.isSceptre(wand)
                    || !(WandFocusItem.attached(wand).getItem() instanceof BuilderFocusItem builder) || BuilderFocusItem.picked(wand).isEmpty())continue;
            var hit=BuilderFocusItem.target(mc.player);if(hit.getType()!=HitResult.Type.BLOCK)continue;
            var blocks=new HashSet<>(builder.selection(wand,hit,mc.player));
            for(var pos:blocks) {
                if(!mc.level.hasChunkAt(pos) || mc.level.isOutsideBuildHeight(pos))continue;
                var p=Vec3.atLowerCornerOf(pos).subtract(camera);pose.pushPose();pose.translate(p.x,p.y,p.z);
                tc4revival.client.ConnectedFaces.render(pos,blocks::contains,blocks::contains,pose,buffers,.3f,.5f,.9f,.2f,true);pose.popPose();
            }
            break;
        }
        buffers.endBatch();
    }
}
