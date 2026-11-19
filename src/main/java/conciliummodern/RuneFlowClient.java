package conciliummodern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class RuneFlowClient {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final List<Rune> RUNES=new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel current;
    private static class Rune {
        Vec3 at,previous,target;int age,index;float scale,offsetX,offsetY,yaw,pitch;
        Rune(RuneFlow.Flight p){at=previous=p.from();target=p.target();var random=current.random;index=random.nextInt(16);scale=(float)(2+random.nextGaussian()*.2);offsetX=random.nextFloat()*.2f;offsetY=-.3f+random.nextFloat()*.6f;var viewer=Minecraft.getInstance().player;if(viewer!=null){yaw=viewer.getYRot();pitch=viewer.getXRot();}}
        void tick(){previous=at;at=at.add(target.subtract(at).normalize().scale(.5));age++;}
    }
    public static void receive(RuneFlow.Flight p){
        var mc=Minecraft.getInstance();if(mc.level==null || !mc.level.dimension().location().equals(p.dimension()) || !Double.isFinite(p.from().distanceToSqr(p.target())) || p.from().distanceToSqr(p.target())>4096)return;
        if(mc.getCameraEntity()==null || mc.getCameraEntity().position().distanceToSqr(p.from())>4096)return;
        if(current!=mc.level){RUNES.clear();current=mc.level;}if(RUNES.size()>=256)RUNES.remove(0);RUNES.add(new Rune(p));
    }
    public static void shadow(RuneFlow.Shadow trail){
        var mc=Minecraft.getInstance();if(mc.level==null || !mc.level.dimension().location().equals(trail.dimension()))return;
        boolean own=mc.player!=null && trail.caster()==mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        for(var p:trail.points())if(Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z) && (!own || p.distanceToSqr(mc.player.getEyePosition())>=4))
            thaumcraft.api.fx.LegacyEffects.emit(mc.level,"shadow",p,Vec3.ZERO,0x010101,2,5,1,0,-.7f);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        var mc=Minecraft.getInstance();if(event.phase!=TickEvent.Phase.END)return;
        if(mc.level!=current){RUNES.clear();current=mc.level;}if(mc.isPaused())return;
        RUNES.forEach(Rune::tick);RUNES.removeIf(r->r.age>=15 || r.at.distanceToSqr(r.target)<3.5);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){RUNES.clear();current=null;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null)return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.runes(TEXTURE);var pose=event.getPoseStack();
        for(var rune:RUNES){var point=rune.previous.lerp(rune.at,event.getPartialTick()).subtract(event.getCamera().getPosition());
            pose.pushPose();pose.translate(point.x,point.y,point.z);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rune.yaw));pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rune.pitch));pose.translate(rune.offsetX,rune.offsetY,-.51);
            float u=rune.index/16f,v=.375f;
            var out=buffers.getBuffer(type);var matrix=pose.last().pose();float half=.15f*rune.scale;
            out.vertex(matrix,-half,-half,0).color(1f,.6f,0f,.8f).uv(u+.0624375f,v+.0624375f).uv2(255).endVertex();
            out.vertex(matrix,-half,half,0).color(1f,.6f,0f,.8f).uv(u+.0624375f,v).uv2(255).endVertex();
            out.vertex(matrix,half,half,0).color(1f,.6f,0f,.8f).uv(u,v).uv2(255).endVertex();
            out.vertex(matrix,half,-half,0).color(1f,.6f,0f,.8f).uv(u,v+.0624375f).uv2(255).endVertex();pose.popPose();
        }
        buffers.endBatch(type);
    }
}
