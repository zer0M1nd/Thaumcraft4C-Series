package tc4revival.client;

import com.mojang.math.Axis;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Short-lived hemispherical scream; follows the source but retains its initial aim. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EldritchSonicEffects {
    private static final ArrayList<Wave> WAVES=new ArrayList<>();
    private static ClientLevel current;
    private EldritchSonicEffects() {}
    public static void emit(Entity source) {
        var mc=Minecraft.getInstance(); if (source.level()!=mc.level) return;
        if (current!=mc.level) { WAVES.clear(); current=mc.level; }
        if (WAVES.size()>=256) WAVES.remove(0);
        WAVES.add(new Wave(source));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; var mc=Minecraft.getInstance();
        if (current!=mc.level) { WAVES.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        WAVES.removeIf(wave->{ wave.previous=wave.position; wave.position=wave.source.getEyePosition(); return wave.age++>=wave.lifetime; });
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var pose=event.getPoseStack(); var camera=event.getCamera().getPosition();
        for (Wave wave:WAVES) {
            var position=wave.previous.lerp(wave.position,event.getPartialTick());
            int frame=Math.min(15,(int)(14*(wave.age+event.getPartialTick())/wave.lifetime)+1);
            var type=TCRenderTypes.infusionRunes(ResourceLocation.parse("thaumcraft:textures/models/ripple"+frame+".png"));
            pose.pushPose(); pose.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-wave.yaw)); pose.mulPose(Axis.XP.rotationDegrees(wave.pitch));
            float height=wave.source.getBbHeight(); pose.translate(0,0,2*height+wave.source.getBbWidth()/2);
            pose.scale(.25f*height,.25f*height,-height);
            EldritchMeshes.draw("hemis","GeoSphere001",pose,buffers,type,220,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,.5f,1);
            pose.popPose(); TCRenderTypes.endBatch(buffers,type);
        }
    }
    private static  class Wave {
        final Entity source; final float yaw,pitch; final int lifetime;
        Vec3 previous,position; int age;
        Wave(Entity source) {
            this.source=source; yaw=source.getYHeadRot(); pitch=source.getXRot(); lifetime=10+source.level().random.nextInt(5);
            previous=position=source.getEyePosition();
        }
    }
}
