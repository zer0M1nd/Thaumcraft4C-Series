package wgmodern;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Retains the legacy sauna overlay and the original sweat/wisp kinematics. */
@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT)
public class WGEffects {
    private static final ResourceLocation OVERLAY=new ResourceLocation("witchinggadgets","textures/gui/steam_overlay.png"),WISP=new ResourceLocation("thaumcraft","textures/misc/particles.png"),SWEAT=new ResourceLocation("minecraft","textures/particle/generic_0.png");
    private static ClientLevel world;private static int saunaUntil;private static final List<Mote> particles=new ArrayList<>();
    public static void sauna(){var mc=Minecraft.getInstance();if(mc.player!=null){if(world!=mc.level){world=mc.level;particles.clear();}saunaUntil=mc.player.tickCount+25;}}
    public static void ember(BlockPos pos){var level=Minecraft.getInstance().level;if(level==null)return;if(world!=level){world=level;particles.clear();saunaUntil=0;}particles.add(new Mote(level,pos.getX()+.5,pos.getY()+.875,pos.getZ()+.5,true,new Vec3(pos.getX()+.3+level.random.nextFloat()*.4,pos.getY()+.5,pos.getZ()+.3+level.random.nextFloat()*.4)));}
    public static void reveal(net.minecraft.world.entity.player.Player player){var mc=Minecraft.getInstance();if(mc.level==null)return;if(world!=mc.level){world=mc.level;particles.clear();saunaUntil=0;}int count=switch(mc.options.particles().get()){case ALL->4;case DECREASED->2;case MINIMAL->0;};if(world.random.nextInt(6)<count)particles.add(new RevealingSpark(world,player.getX()+player.getRandom().nextFloat()-.5,player.getY()+1+player.getRandom().nextFloat()-.5,player.getZ()+player.getRandom().nextFloat()-.5));}
    public static void targeted(Vec3 from,Vec3 to,int color,float size){var level=Minecraft.getInstance().level;if(level==null)return;if(world!=level){world=level;particles.clear();saunaUntil=0;}particles.add(new TargetedWisp(level,from,to,color,size));}
    public static void renderSteam(GuiGraphics g,int width,int height){var p=Minecraft.getInstance().player;if(p==null||p.tickCount>saunaUntil||world!=Minecraft.getInstance().level)return;com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();g.setColor(1,1,1,1);tc4revival.client.LegacyGui.blit(g,OVERLAY,0,0,0,0,width,height,width,height);com.mojang.blaze3d.systems.RenderSystem.disableBlend();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(world!=mc.level){world=mc.level;particles.clear();saunaUntil=0;}if(world==null||mc.isPaused())return;if(mc.player!=null&&mc.player.tickCount<saunaUntil&&world.random.nextInt(3)==0){var p=mc.player;particles.add(new Mote(world,p.getX()-.2+world.random.nextFloat()/2,p.getY()+.5+world.random.nextFloat(),p.getZ()-.2+world.random.nextFloat()/2,false,null));}particles.forEach(Mote::tick);particles.removeIf(p->!p.isAlive());}
    // Legacy ParticleEngine uses a 1/255 alpha cutoff for wisps and sparks, unlike vanilla .1.
    @SubscribeEvent public static void render(RenderLevelStageEvent e){if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world==null||particles.isEmpty())return;var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var additive=tc4revival.client.TCRenderTypes.primalSprite(WISP);var sweat=tc4revival.client.TCRenderTypes.softParticle(SWEAT);for(var mote:particles)mote.draw(e.getPoseStack(),buffers.getBuffer(mote.ember?additive:sweat),e.getCamera(),e.getPartialTick());buffers.endBatch(additive);buffers.endBatch(sweat);}
    private static void quad(VertexConsumer out,org.joml.Matrix4f matrix,float half,float u,float v,float U,float V,float red,float green,float blue,float alpha,int light){out.vertex(matrix,-half,-half,0).color(red,green,blue,alpha).uv(U,V).uv2(light).endVertex();out.vertex(matrix,-half,half,0).color(red,green,blue,alpha).uv(U,v).uv2(light).endVertex();out.vertex(matrix,half,half,0).color(red,green,blue,alpha).uv(u,v).uv2(light).endVertex();out.vertex(matrix,half,-half,0).color(red,green,blue,alpha).uv(u,V).uv2(light).endVertex();}
    public static class Mote extends Particle {
        private final boolean ember;private final float size;
        public Mote(ClientLevel level,double x,double y,double z,boolean ember,Vec3 target){super(level,x,y,z);this.ember=ember;friction=.98f;gravity=ember?-.025f:.06f;setSize(ember?.1f:.01f,ember?.1f:.01f);size=ember?(1+random.nextFloat())*.5f:(1+random.nextFloat())*.1f;lifetime=ember?(int)(36/(random.nextDouble()*.3+.7)):20;rCol=ember?.7f+random.nextFloat()*.3f:.2f;gCol=.2f;bCol=ember?.2f:.3f+random.nextFloat()*.5f;xd=yd=zd=0;if(target!=null){xd=(target.x-x)/lifetime;yd=(target.y-y)/lifetime;zd=(target.z-z)/lifetime;}}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera cam,float partial){}
        public void draw(PoseStack pose,VertexConsumer out,Camera camera,float partial){double px=xo+(x-xo)*partial-camera.getPosition().x,py=yo+(y-yo)*partial-camera.getPosition().y,pz=zo+(z-zo)*partial-camera.getPosition().z;pose.pushPose();pose.translate(px,py,pz);pose.mulPose(camera.rotation());float half=ember?.5f*size*(lifetime-age)/(float)lifetime:size;quad(out,pose.last().pose(),half,0,ember?.875f:0,ember?.125f:1,1,rCol,gCol,bCol,ember?.5f:1,ember?LightTexture.FULL_BRIGHT:getLightColor(partial));pose.popPose();}
    }
    public static class TargetedWisp extends Mote {
        private final float size;
        public TargetedWisp(ClientLevel level,Vec3 from,Vec3 target,int color,float scale){super(level,from.x,from.y,from.z,true,target);gravity=0;hasPhysics=false;size=(1+random.nextFloat())*scale;rCol=(color>>16&255)/255f;gCol=(color>>8&255)/255f;bCol=(color&255)/255f;}
        @Override public void draw(PoseStack pose,VertexConsumer out,Camera camera,float partial){pose.pushPose();pose.translate(xo+(x-xo)*partial-camera.getPosition().x,yo+(y-yo)*partial-camera.getPosition().y,zo+(z-zo)*partial-camera.getPosition().z);pose.mulPose(camera.rotation());quad(out,pose.last().pose(),.5f*size*(lifetime-age)/lifetime,0,.875f,.125f,1,rCol,gCol,bCol,.5f,LightTexture.FULL_BRIGHT);pose.popPose();}
    }
    public static class RevealingSpark extends Mote {
        private final float scale;
        public RevealingSpark(ClientLevel level,double x,double y,double z){super(level,x,y,z,true,null);lifetime=18;gravity=0;friction=.908f;hasPhysics=false;scale=(random.nextFloat()*.5f+.5f)*2;rCol=.75f+random.nextFloat()*.25f;gCol=.25f+random.nextFloat()*.25f;bCol=.75f+random.nextFloat()*.25f;}
        @Override public void draw(PoseStack pose,VertexConsumer out,Camera camera,float partial){pose.pushPose();pose.translate(xo+(x-xo)*partial-camera.getPosition().x,yo+(y-yo)*partial-camera.getPosition().y,zo+(z-zo)*partial-camera.getPosition().z);pose.mulPose(camera.rotation());float u=((16+age/6)%4)/16f;quad(out,pose.last().pose(),.1f*scale*(lifetime-age+1)/lifetime,u,.25f,u+.0624375f,.3124375f,rCol,gCol,bCol,.75f,LightTexture.FULL_BRIGHT);pose.popPose();}
    }
}
