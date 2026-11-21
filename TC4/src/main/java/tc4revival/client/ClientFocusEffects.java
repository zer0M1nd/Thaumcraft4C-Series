package tc4revival.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import tc4revival.network.FocusEffectPacket;

/** Buffered versions of the original bolt, sparkle, ground spark and node-atlas burst effects. */
@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class ClientFocusEffects {
    public static void mirrorTransfer(Level level,BlockPos pos,net.minecraft.core.Direction face) { MirrorTransferParticles.emit(level,pos,face); }
    private static final ResourceLocation PARTICLES = texture("particles"), SPARKS = texture("particles2"), NODES = texture("nodes");
    private static final ResourceLocation[] BOLT_TEXTURES = {texture("p_large"), texture("p_small")};
    private static final List<LightningGeometry> BOLTS = new ArrayList<>();
    private static final List<Sprite> SPRITES = new ArrayList<>();
    private static ClientLevel currentLevel;
    private enum Kind { SPARKLE, SPARK, BURST, MANIPULATOR, WISP }
    private static  class Sprite {
        final Kind kind;
        final float scale, red, green, blue, gravity;
        final int lifetime, row;
        final boolean flip;
        Vec3 position, previous;
        double speedY;
        int age,delay;
        Sprite(Kind kind, Vec3 position, float scale, float red, float green, float blue, float gravity, int lifetime, int row, boolean flip) {
            this.kind = kind; this.position = position; previous = position; this.scale = scale;
            this.red = red; this.green = green; this.blue = blue; this.gravity = gravity; this.lifetime = lifetime; this.row = row; this.flip = flip;
        }
        void tick() {
            previous = position; age++;
            if (kind == Kind.SPARKLE) { speedY -= .04 * gravity; position = position.add(0, speedY, 0); speedY *= .9080000019073486; }
            if (kind == Kind.MANIPULATOR && row == 1) { position = position.add(0, speedY, 0); speedY *= .98; }
        }
    }
    public static void wispBirth(Level level,Vec3 point) {
        if(prepare(level))sprite(new Sprite(Kind.BURST,point,(level.random.nextFloat()*.5f+.5f)*2,1,1,1,0,31,0,false));
    }
    public static void wispMote(Level level,Vec3 point,int color) {
        if(!prepare(level))return;var viewer=Minecraft.getInstance().getCameraEntity();int distance=Minecraft.useFancyGraphics()?50:25;
        if(viewer==null || viewer.distanceToSqr(point)>distance*distance)return;
        float red=(color>>16&255)/255f;
        sprite(new Sprite(Kind.WISP,point,(level.random.nextFloat()*.5f+.5f)*.2f,red==0?1:red,(color>>8&255)/255f,(color&255)/255f,0,(int)(36/(level.random.nextDouble()*.3+.7)),0,false));
    }
    private ClientFocusEffects() {}
    private static ResourceLocation texture(String name) { return ResourceLocation.parse("thaumcraft:textures/misc/" + name + ".png"); }
    private static boolean prepare(Level level) {
        var mc = Minecraft.getInstance();
        if (level == null || level != mc.level) return false;
        if (currentLevel != mc.level) { BOLTS.clear(); SPRITES.clear(); currentLevel = mc.level; }
        return true;
    }
    public static void receive(FocusEffectPacket packet, Connection connection) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.getConnection().getConnection() != connection || !connection.isConnected()
                || !prepare(mc.level) || !mc.level.dimension().location().equals(packet.dimension())) return;
        if (packet.kind()==FocusEffectPacket.Kind.MIST_SHORT) EldritchMist.refresh();
        else if (packet.kind()==FocusEffectPacket.Kind.MIST_LONG) EldritchMist.longMist();
        else if (packet.kind()==FocusEffectPacket.Kind.WARP_EVENT) {
            WarpVignette.trigger(100);
            if (mc.player!=null) mc.player.playSound(tc4revival.content.TCSounds.HEARTBEAT.get(),1,1);
        }
        else if (packet.kind()==FocusEffectPacket.Kind.WARP_RELIEF)
            ClientScanFeedback.notice(net.minecraft.client.resources.language.I18n.get("tc.removewarpsticky"));
        else if (packet.kind()==FocusEffectPacket.Kind.SONIC) {
            var source=mc.level.getEntity((int)packet.seed()); if (source!=null) EldritchSonicEffects.emit(source);
        }
        else if (packet.kind()==FocusEffectPacket.Kind.BLOCK_ARC) {
            var random=mc.level.random; boolean crimson=packet.seed()==1;
            float r=.3f-random.nextFloat()*.1f,b=.5f+random.nextFloat()*.2f;
            if (crimson) { r=.5f+random.nextFloat()*.2f; b=0; }
            EldritchArcEffects.emit(mc.level,packet.start(),packet.end(),r,0,b,packet.scale());
        }
        else if (packet.kind() == FocusEffectPacket.Kind.LIGHTNING) bolt(packet.start(), packet.end(), packet.seed());
        else if (packet.kind() == FocusEffectPacket.Kind.NODE_ZAP) {
            infusionBolt(packet.start(),packet.end());
            mc.level.playLocalSound(packet.start().x,packet.start().y,packet.start().z,tc4revival.content.TCSounds.ZAP.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,.1f,1+mc.level.random.nextFloat()*.2f,false);
        }
        else if (packet.kind() == FocusEffectPacket.Kind.BLOCK_SPARKLE) blockSparkle(mc.level, BlockPos.containing(packet.start()), (int) packet.seed(), Math.max(1,Math.min(16,(int)packet.scale())));
        else if (packet.kind() == FocusEffectPacket.Kind.ELEMENTAL_BLOCK_BUBBLE) CrucibleEffects.elementalBlockBubble(mc.level,BlockPos.containing(packet.start()));
        else {
            float scale = (mc.level.random.nextFloat() * .5f + .5f) * 2 * packet.scale();
            sprite(new Sprite(Kind.BURST, packet.start(), scale, 1, 1, 1, 0, 31, 0, false));
        }
    }
    private static void bolt(Vec3 from, Vec3 to, long seed) {
        if (from.distanceToSqr(to) < 1e-8) return; // Coincident endpoints produced invalid old renderer vertices.
        BOLTS.add(new LightningGeometry(from, to, seed));
    }
    public static void infusionBolt(Vec3 from,Vec3 to) {
        var level=Minecraft.getInstance().level;
        if (level!=null && prepare(level) && from.distanceToSqr(to)>=1e-8)
            BOLTS.add(new LightningGeometry(from,to,level.random.nextLong(),10,4,5,true));
    }
    public static void nodeConversion(Level level,BlockPos converter,boolean stabilized) {
        if(!prepare(level))return;
        var random=level.random;var end=Vec3.atCenterOf(converter.below());
        if(random.nextBoolean()) {
            var start=new Vec3(converter.getX()+.25f+random.nextFloat()*.5f,converter.getY()+.5f,converter.getZ()+.25f+random.nextFloat()*.5f);
            BOLTS.add(new LightningGeometry(start,end,random.nextLong(),10,4,5,true));
        }
        if(random.nextBoolean()&&stabilized) {
            var start=new Vec3(converter.getX()+.25f+random.nextFloat()*.5f,converter.getY()-1.5f,converter.getZ()+.25f+random.nextFloat()*.5f);
            BOLTS.add(new LightningGeometry(start,end,random.nextLong(),10,4,5,true));
        }
    }
    private static void sprite(Sprite value) { if (SPRITES.size() >= 2000) SPRITES.remove(0); SPRITES.add(value); }
    public static void shock(Player player, Entity target) {
        if (!prepare(player.level())) return;
        var look = player.getLookAngle();
        var eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye, eye.add(look.scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        var end = eye.add(look.scale(10));
        if (hit.getType() != HitResult.Type.MISS) { end = hit.getLocation(); sparkles(end, .3f); }
        if (target != null) { end = target.getBoundingBox().getCenter(); sparkles(end, .6f); }
        double yaw = Math.toRadians(player.getYRot());
        double y = player == Minecraft.getInstance().player ? player.getEyeY() - .06 : player.getBoundingBox().minY + player.getBbHeight() / 2 + .25;
        int side=ContinuousFocusBeam.castingSide(player);
        var start = new Vec3(player.getX() - Math.cos(yaw) * .06 * side, y, player.getZ() - Math.sin(yaw) * .06 * side).add(look.scale(.3));
        bolt(start, end, player.level().random.nextLong());
    }
    private static void sparkles(Vec3 center, float spread) {
        var mc = Minecraft.getInstance(); var random = mc.level.random;
        int chance = switch (mc.options.particles().get()) { case ALL -> 4; case DECREASED -> 2; case MINIMAL -> 0; };
        for (int i = 0; i < 5; i++) {
            var at = center.add((random.nextFloat() - random.nextFloat()) * spread,
                    (random.nextFloat() - random.nextFloat()) * spread, (random.nextFloat() - random.nextFloat()) * spread);
            float size = 2 + random.nextFloat(), gravity = .05f + random.nextFloat() * .05f;
            if (random.nextInt(6) >= chance) continue;
            sprite(new Sprite(Kind.SPARKLE, at, (random.nextFloat() * .5f + .5f) * 2 * size,
                    .2f, .2f, .7f + random.nextFloat() * .3f, gravity, 18, 0, false));
        }
    }
    public static void groundSpark(Level level, BlockPos position) {
        if (!prepare(level)) return;
        var random = level.random;
        float extra = random.nextFloat() * .33f;
        var at = new Vec3(position.getX() + random.nextFloat(), position.getY() + .1515f + extra / 2, position.getZ() + random.nextFloat());
        sprite(new Sprite(Kind.SPARK, at, .33f + extra, .65f + random.nextFloat() * .1f, 1, 1, 0,
                5 + random.nextInt(5), random.nextInt(3), random.nextBoolean()));
    }
    public static void infusedOreHit(Level level,BlockPos pos,tc4revival.content.ShardType type){
        if(!prepare(level))return;var random=level.random;int count=switch(Minecraft.getInstance().options.particles().get()){case ALL->3;case DECREASED->1;case MINIMAL->0;};
        int color=type.tint();for(int i=0;i<count;i++)sprite(new Sprite(Kind.SPARKLE,Vec3.atLowerCornerOf(pos).add(random.nextFloat(),random.nextFloat(),random.nextFloat()),1.75f,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.1f,3+random.nextInt(3),0,false));
    }
    public static void crystalSpark(Level level,BlockPos position,tc4revival.content.ShardType type) {
        if (!prepare(level)) return;
        var random=level.random;
        var at=Vec3.atLowerCornerOf(position).add(.3+random.nextFloat()*.4,.3+random.nextFloat()*.4,.3+random.nextFloat()*.4);
        float size=.2f+random.nextFloat()*.1f;
        int lifetime=5+random.nextInt(5),row=random.nextInt(3); boolean flip=random.nextBoolean();
        int color=(type==tc4revival.content.ShardType.BALANCED ? tc4revival.content.ShardType.values()[random.nextInt(6)] : type).tint();
        sprite(new Sprite(Kind.SPARK,at,size,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,0,lifetime,row,flip));
    }
    public static void silverwoodSparkle(Level level,BlockPos position) {
        if (!prepare(level)) return;
        var random=level.random;
        var at=Vec3.atCenterOf(position).add(random.nextFloat()-random.nextFloat(),random.nextFloat()-random.nextFloat(),random.nextFloat()-random.nextFloat());
        int chance=switch (Minecraft.getInstance().options.particles().get()) { case ALL->4; case DECREASED->2; case MINIMAL->0; };
        if (random.nextInt(6)<chance) sprite(new Sprite(Kind.SPARKLE,at,(random.nextFloat()*.5f+.5f)*4,.2f,.5f+random.nextFloat()*.3f,.6f+random.nextFloat()*.3f,0,18,0,false));
    }
    public static void manipulator(Level level, BlockPos position) {
        if (!prepare(level)) return;
        var random = level.random;
        var at = Vec3.atCenterOf(position).add((random.nextFloat() - random.nextFloat()) * .3,
                .75 + (random.nextFloat() - random.nextFloat()) * .3, (random.nextFloat() - random.nextFloat()) * .3);
        sprite(new Sprite(Kind.MANIPULATOR, at, .7f + random.nextFloat() * .4f, .5f + random.nextFloat() * .4f,
                1 - random.nextFloat() * .4f, 1 - random.nextFloat() * .4f, 0, 6 + random.nextInt(5), 0, false));
    }
    public static void arcEndpoint(Level level,Vec3 point,float r,float g,float b) {
        if (prepare(level)) sprite(new Sprite(Kind.SPARKLE,point,(level.random.nextFloat()*.5f+.5f)*6,r,g,b,0,6,0,false));
    }
    public static void blueSparkle(Level level, Vec3 point) {
        if (!prepare(level)) return;
        int chance = switch (Minecraft.getInstance().options.particles().get()) { case ALL -> 4; case DECREASED -> 2; case MINIMAL -> 0; };
        if (level.random.nextInt(6) < chance) sprite(new Sprite(Kind.SPARKLE, point, (level.random.nextFloat() * .5f + .5f) * 3,
                .2f, .2f, .7f + level.random.nextFloat() * .3f, .1f, 18, 0, false));
    }
    public static void levitatorSparkle(Level level,Vec3 point) {
        if (!prepare(level)) return;
        int chance=switch(Minecraft.getInstance().options.particles().get()) { case ALL->4; case DECREASED->2; case MINIMAL->0; };
        if (level.random.nextInt(6)<chance) sprite(new Sprite(Kind.SPARKLE,point,level.random.nextFloat()*.5f+.5f,.2f,.7f+level.random.nextFloat()*.3f,.2f,-.3f,18,0,false));
    }
    public static void followingSparkle(Level level,Vec3 point,int effect) {
        if (!prepare(level)) return;
        int chance=switch(Minecraft.getInstance().options.particles().get()) { case ALL->4; case DECREASED->2; case MINIMAL->0; };
        if (level.random.nextInt(6)<chance) {
            float size=(level.random.nextFloat()*.5f+.5f)*3,bright=.7f+level.random.nextFloat()*.3f;
            sprite(new Sprite(Kind.SPARKLE,point,size,.2f,effect==2?.2f:bright,effect==2?bright:.2f,0,18,0,false));
        }
    }
    public static void blockSparkle(Level level, BlockPos pos, int color) {
        blockSparkle(level,pos,color,1);
    }
    public static void blockSparkle(Level level, BlockPos pos, int color,int baseCount) {
        if (!prepare(level)) return;
        int count = switch (Minecraft.getInstance().options.particles().get()) { case ALL -> baseCount*2; case DECREASED -> baseCount; case MINIMAL -> 0; };
        var random = level.random;
        for (int i = 0; i < count; i++) {
            float red=color==-9999 ? .33f+random.nextFloat()*.67f : (color>>16&255)/255f;
            float green=color==-9999 ? .33f+random.nextFloat()*.67f : (color>>8&255)/255f;
            float blue=color==-9999 ? .33f+random.nextFloat()*.67f : (color&255)/255f;
            var point = Vec3.atLowerCornerOf(pos).add(-.1 + random.nextFloat() * 1.2, -.1 + random.nextFloat() * 1.2, -.1 + random.nextFloat() * 1.2);
            double speedY=random.nextFloat()*.02;
            red+=-.2f+random.nextFloat()*.4f; green+=-.2f+random.nextFloat()*.4f; blue+=-.2f+random.nextFloat()*.4f;
            int duration=5+random.nextInt(8),delay=random.nextInt(10);
            var mote = new Sprite(Kind.MANIPULATOR, point, .7f + random.nextFloat() * .4f,
                    red,green,blue,0,duration+delay,1,false);
            mote.speedY=speedY; mote.delay=delay; sprite(mote);
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { BOLTS.clear(); SPRITES.clear(); currentLevel = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || !prepare(mc.level) || mc.isPaused()) return;
        BOLTS.forEach(LightningGeometry::tick); BOLTS.removeIf(LightningGeometry::expired);
        SPRITES.forEach(Sprite::tick); SPRITES.removeIf(sprite -> sprite.age >= sprite.lifetime);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !prepare(mc.level)) return;
        var pose = event.getPoseStack(); var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        var view = new org.joml.Vector3f(0, 0, 1).rotate(event.getCamera().rotation());
        var facing = new Vec3(view.x(), view.y(), view.z());
        for (int pass = 0; pass < 2; pass++) {
            var type = TCRenderTypes.runes(BOLT_TEXTURES[pass]); var vertices = TCRenderTypes.buffer(buffers,type);
            for (var bolt : BOLTS) {
                double limit = Minecraft.useFancyGraphics() ? 100 : 50;
                if (bolt.origin.distanceToSqr(camera) > limit * limit) continue;
                float fade = Math.max(0, bolt.age) / (float) bolt.lifetime;
                float alpha = pass == 0 ? (1 - fade) * .4f : 1 - fade * .5f;
                int visible = bolt.visibleSegments(event.getPartialTick());
                for (var edge : bolt.edges) {
                    if (edge.progress > visible) continue;
                    float width = (bolt.nodeColor ? .03f : .125f) * ((float) edge.start().distanceTo(camera) / 5 + 1) * (1 + edge.brightness) / 2;
                    var left = edge.join(true, facing, width); var right = edge.join(false, facing, width);
                    var from = edge.start().subtract(camera); var to = edge.end().subtract(camera);
                    line(vertices, pose.last().pose(), from, to, left, right, .5f, .5f, alpha * edge.brightness, pass, bolt.nodeColor);
                    if (edge.next == null) line(vertices, pose.last().pose(), to, to.add(edge.direction().normalize().scale(width)), right, right, .5f, 0, alpha * edge.brightness, pass, bolt.nodeColor);
                    if (edge.previous == null) line(vertices, pose.last().pose(), from.subtract(edge.direction().normalize().scale(width)), from, left, left, 0, .5f, alpha * edge.brightness, pass, bolt.nodeColor);
                }
            }
            TCRenderTypes.endBatch(buffers,type);
        }
        for (var kind : Kind.values()) {
            var type = kind == Kind.BURST ? TCRenderTypes.softParticles(NODES) : TCRenderTypes.runes(kind == Kind.SPARK ? SPARKS : PARTICLES);
            var vertices = TCRenderTypes.buffer(buffers,type);
            for (var sprite : SPRITES) {
                if (sprite.kind != kind || sprite.age<sprite.delay) continue;
                var at = sprite.previous.lerp(sprite.position, event.getPartialTick()).subtract(camera);
                pose.pushPose(); pose.translate(at.x, at.y, at.z); pose.mulPose(event.getCamera().rotation());
                float u, v, step, half = sprite.scale;
                if (kind == Kind.BURST) { u = sprite.age / 32f; v = .96875f; step = .03125f; }
                else if (kind == Kind.WISP) { u=0;v=.875f;step=.125f;float ageScale=sprite.age/(float)(sprite.lifetime/2);half*=.5f*Math.max(0,ageScale>1?2-ageScale:ageScale); }
                else if (kind == Kind.MANIPULATOR) { u = Math.min(8, (int) (sprite.age * 9f / sprite.lifetime)) / 16f; v = .4375f; step = .0624375f; half *= .1f; }
                else if (kind == Kind.SPARK) { u = (int) (sprite.age * 7f / sprite.lifetime) / 8f; v = sprite.row / 8f; step = .125f; }
                else { u = (sprite.age / 6 % 4) / 16f; v = .25f; step = .0624375f; half *= .1f * (sprite.lifetime - sprite.age + 1) / sprite.lifetime; }
                int light = kind == Kind.SPARK || kind == Kind.MANIPULATOR ? net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, BlockPos.containing(sprite.position)) : 240;
                quad(vertices, pose.last().pose(), half, sprite.flip ? u + step : u, v, sprite.flip ? u : u + step, v + step,
                        sprite.red, sprite.green, sprite.blue, kind == Kind.WISP ? .5f : kind == Kind.SPARK ? .8f : kind == Kind.MANIPULATOR ? (sprite.row == 1 ? .9f : .8f)*(sprite.age <= 1 || sprite.age >= sprite.lifetime - 1 ? .5f : 1) : 1, light);
                pose.popPose();
            }
            TCRenderTypes.endBatch(buffers,type);
        }
    }
    private static void line(VertexConsumer out, Matrix4f pose, Vec3 from, Vec3 to, Vec3 firstWidth, Vec3 secondWidth, float u1, float u2, float alpha, int pass, boolean nodeColor) {
        vertex(out, pose, to.subtract(secondWidth), u2, 0, alpha, pass,nodeColor);
        vertex(out, pose, from.subtract(firstWidth), u1, 0, alpha, pass,nodeColor);
        vertex(out, pose, from.add(firstWidth), u1, 1, alpha, pass,nodeColor);
        vertex(out, pose, to.add(secondWidth), u2, 1, alpha, pass,nodeColor);
    }
    private static void vertex(VertexConsumer out, Matrix4f pose, Vec3 at, float u, float v, float alpha, int pass, boolean nodeColor) {
        float red=nodeColor?(pass==0?.6f:1):.1f;
        float green=nodeColor?(pass==0?.3f:.6f):.1f;
        out.vertex(pose, (float) at.x, (float) at.y, (float) at.z).color(red, green, pass == 0 ? .6f : 1, alpha).uv(u, v).uv2(0xf000f0).endVertex();
    }
    static void quad(VertexConsumer out, Matrix4f pose, float half, float u1, float v1, float u2, float v2, float red, float green, float blue, float alpha, int light) {
        red=net.minecraft.util.Mth.clamp(red,0,1); green=net.minecraft.util.Mth.clamp(green,0,1); blue=net.minecraft.util.Mth.clamp(blue,0,1); alpha=net.minecraft.util.Mth.clamp(alpha,0,1);
        out.vertex(pose, -half, -half, 0).color(red, green, blue, alpha).uv(u2, v2).uv2(light).endVertex();
        out.vertex(pose, -half, half, 0).color(red, green, blue, alpha).uv(u2, v1).uv2(light).endVertex();
        out.vertex(pose, half, half, 0).color(red, green, blue, alpha).uv(u1, v1).uv2(light).endVertex();
        out.vertex(pose, half, -half, 0).color(red, green, blue, alpha).uv(u1, v2).uv2(light).endVertex();
    }
}
