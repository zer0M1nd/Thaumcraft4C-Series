package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class ArcaneBoreSparkles {
    private static final java.util.ArrayList<Spark> SPARKS=new java.util.ArrayList<>();
    private static ClientLevel current;
    private ArcaneBoreSparkles() {}
    static void add(ClientLevel level,BlockPos source,BlockPos destination,BlockState state) {
        if (current!=level) { SPARKS.clear(); current=level; }
        SPARKS.add(new Spark(level,source,Vec3.atCenterOf(destination),state));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        if (current!=Minecraft.getInstance().level) { SPARKS.clear(); current=null; return; }
        SPARKS.forEach(Spark::tick); SPARKS.removeIf(spark->!spark.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || SPARKS.isEmpty()) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var type=TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/misc/particles.png"));
        var out=TCRenderTypes.buffer(buffers,type);
        for (var spark:SPARKS) spark.renderWorld(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());
        TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Spark extends ArcaneBoreDebris {
        Spark(ClientLevel level,BlockPos source,Vec3 target,BlockState state) {
            super(level,source,target,state); quadSize=.1f*(random.nextFloat()*.5f+.5f); rCol=.2f; gCol=.6f+random.nextFloat()*.3f; bCol=.2f;
        }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { renderWorld(out,camera,partial,new PoseStack()); }
        void renderWorld(VertexConsumer out,Camera camera,float partial,PoseStack worldView) {
            var pose=WorldEffectPose.copy(worldView); var view=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z); pose.mulPose(camera.rotation());
            float u=(age%4)/16f,size=quadSize*(Mth.sin(age/3f)*.5f+1);
            ClientFocusEffects.quad(out,pose.last().pose(),size,u+.0624375f,.25f,u,.3124375f,rCol,gCol,bCol,1,240);
        }
    }
}
