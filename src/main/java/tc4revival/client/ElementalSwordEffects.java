package tc4revival.client;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class ElementalSwordEffects {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final ArrayList<Spiral> SPIRALS=new ArrayList<>();
    private static ClientLevel current;
    private static  class Spiral {
        final Vec3 center; final int floor,start,lifetime; final float size; int age;
        Spiral(Player player,int floor) {
            center=new Vec3(player.getX(),player.getBoundingBox().minY+player.getBbHeight()/2,player.getZ());
            this.floor=floor; start=player.getRandom().nextInt(360); size=(player.getRandom().nextFloat()*.5f+.5f)*2;
            lifetime=20+player.getRandom().nextInt(10);
        }
    }
    public static void emit(Player player) {
        var mc=Minecraft.getInstance(); if (player.level()!=mc.level) return;
        if (current!=mc.level) { SPIRALS.clear(); current=mc.level; }
        int floor=player.onGround()?Mth.floor(player.getBoundingBox().minY):(int)(player.getBoundingBox().minY-2);
        for (int i=0;i<5;i++) SPIRALS.add(new Spiral(player,floor));
        if (player.onGround()) {
            float angle=player.getRandom().nextFloat()*360/180*(float)Math.PI;
            player.level().addParticle(ParticleTypes.SMOKE,player.getX(),player.getBoundingBox().minY+(double).1f,player.getZ(),-Mth.sin(angle)/5,0,Mth.cos(angle)/5);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance(); if (current!=mc.level) { SPIRALS.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        SPIRALS.removeIf(spiral->spiral.age++>=spiral.lifetime);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.softTranslucent(TEXTURE); var out=TCRenderTypes.buffer(buffers,type);
        var pose=event.getPoseStack(); var camera=event.getCamera();
        for (var spiral:SPIRALS) {
            float progress=(spiral.age+event.getPartialTick())/spiral.lifetime;
            float longitude=(spiral.start+720*progress)/180*(float)Math.PI,latitude=(90-180*progress)/180*(float)Math.PI;
            var position=spiral.center.add(-Mth.sin(longitude)*Mth.cos(latitude)*1.5,-Mth.sin(latitude)*1.5,Mth.cos(longitude)*Mth.cos(latitude)*1.5);
            position=new Vec3(position.x,Math.max(position.y,spiral.floor+.1f),position.z);
            var offset=position.subtract(camera.getPosition()); pose.pushPose(); pose.translate(offset.x,offset.y,offset.z); pose.mulPose(camera.rotation());
            int frame=(int)(1+spiral.age/(float)spiral.lifetime*4); float u=frame/16f;
            ClientFocusEffects.quad(out,pose.last().pose(),.15f*spiral.size,u,0,u+.0624375f,.0624375f,221/255f,221/255f,221/255f,
                    .66f*Math.max(0,(spiral.lifetime-spiral.age+1)/(float)spiral.lifetime),net.minecraft.client.renderer.LevelRenderer.getLightColor(current,BlockPos.containing(spiral.center)));
            pose.popPose();
        }
        TCRenderTypes.endBatch(buffers,type);
    }
}
