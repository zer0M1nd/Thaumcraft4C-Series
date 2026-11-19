package conciliummodern;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.TCRenderTypes;

/** Original conversion fragments and type-10 colored fractal lightning, on the rendering side only. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class ConciliumParticleEffects {
    private static ClientLevel current;
    private record Bolt(ConciliumLightningGeometry geometry,int color){}
    private static final ArrayList<Bolt> BOLTS=new ArrayList<>();
    private static final ArrayList<CrucibleBubble> BUBBLES=new ArrayList<>();
    private static final ResourceLocation[] TEXTURES={ResourceLocation.parse("thaumcraft:textures/misc/p_large.png"),ResourceLocation.parse("thaumcraft:textures/misc/p_small.png")};
    private static void world(ClientLevel level){if(current!=level){BOLTS.clear();BUBBLES.clear();current=level;}}
    public static void crucibleBubble(net.minecraft.world.level.Level level,Vec3 at,int color){
        var mc=Minecraft.getInstance();if(level!=mc.level||!(level instanceof ClientLevel client))return;world(client);
        int range=Minecraft.useFancyGraphics()?50:25;if(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(at)>range*range)return;
        if(BUBBLES.size()>=4000)BUBBLES.remove(0);BUBBLES.add(new CrucibleBubble(client,at,color));
    }
    /** FXBubble, distinct from the slimy liquid-death animation. */
    private static final class CrucibleBubble extends net.minecraft.client.particle.Particle {
        private int frame=16;private final float radius;
        CrucibleBubble(ClientLevel level,Vec3 at,int color){
            super(level,at.x,at.y,at.z);radius=.1f*(random.nextFloat()*.5f+.5f)*2*(random.nextFloat()*.3f+.2f);
            xd=(random.nextDouble()*2-1)*.02;yd=random.nextDouble()*.02;zd=(random.nextDouble()*2-1)*.02;
            lifetime=(int)(3+8/(random.nextDouble()*.8+.2));setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);hasPhysics=false;setSize(.02f,.02f);
        }
        @Override public void tick(){xo=x;yo=y;zo=z;yd+=.002;xd+=(random.nextFloat()-random.nextFloat())*.01;zd+=(random.nextFloat()-random.nextFloat())*.01;setPos(x+xd,y+yd,z+zd);xd*=.85;yd*=.85;zd*=.85;if(lifetime--<=0)remove();else if(lifetime<=2)frame++;}
        @Override public net.minecraft.client.particle.ParticleRenderType getRenderType(){return net.minecraft.client.particle.ParticleRenderType.CUSTOM;}
        @Override public void render(com.mojang.blaze3d.vertex.VertexConsumer out,net.minecraft.client.Camera camera,float partial){}
        void renderWorld(com.mojang.blaze3d.vertex.VertexConsumer out,RenderLevelStageEvent event){
            var pose=event.getPoseStack();var camera=event.getCamera();var eye=camera.getPosition();float partial=event.getPartialTick();pose.pushPose();
            pose.translate(net.minecraft.util.Mth.lerp(partial,xo,x)-eye.x,net.minecraft.util.Mth.lerp(partial,yo,y)-eye.y,net.minecraft.util.Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(camera.rotation());
            float u=frame%16/16f,v=frame/16/16f;var matrix=pose.last().pose();
            out.vertex(matrix,-radius,-radius,0).color(rCol,gCol,bCol,1).uv(u+.0624375f,v+.0624375f).uv2(240).endVertex();
            out.vertex(matrix,-radius,radius,0).color(rCol,gCol,bCol,1).uv(u+.0624375f,v).uv2(240).endVertex();
            out.vertex(matrix,radius,radius,0).color(rCol,gCol,bCol,1).uv(u,v).uv2(240).endVertex();
            out.vertex(matrix,radius,-radius,0).color(rCol,gCol,bCol,1).uv(u,v+.0624375f).uv2(240).endVertex();pose.popPose();
        }
    }
    private static boolean connected(ResourceLocation dimension,Connection connection){var mc=Minecraft.getInstance();return mc.level!=null&&mc.getConnection()!=null&&mc.getConnection().getConnection()==connection&&connection.isConnected()&&mc.level.dimension().location().equals(dimension);}
    public static void taint(ConciliumArcNetwork.Taint packet,Connection connection){
        if(!connected(packet.dimension(),connection)||!Double.isFinite(packet.at().lengthSqr()))return;
        var mc=Minecraft.getInstance();for(int i=0;i<50;i++)mc.particleEngine.add(new TaintParticle(mc.level,packet.at()));
    }
    private static final class TaintParticle extends net.minecraft.client.particle.BreakingItemParticle {
        private final float initialAlpha;
        TaintParticle(ClientLevel level,Vec3 at){
            super(level,at.x,at.y+level.random.nextFloat(),at.z,new ItemStack(Items.SLIME_BALL));
            boolean bright=random.nextBoolean();setColor(bright?.6f:.3f,0,.3f);initialAlpha=bright?.4f:.6f;setAlpha(initialAlpha);
            var velocity=new Vec3(random.nextDouble()*2-1,random.nextDouble()*2-1,random.nextDouble()*2-1).normalize().scale((random.nextDouble()+random.nextDouble()+1)*.15*.9640000000596046);
            xd=velocity.x;yd=velocity.y+.10000000149011612;zd=velocity.z;lifetime=(int)(66/(random.nextFloat()*.9f+.1f));
        }
        private float fade(){return Math.max(0,1-age/(float)lifetime);}
        @Override public void move(double mx,double my,double mz){
            var moved=net.minecraft.world.entity.Entity.collideBoundingBox(null,new Vec3(mx,my,mz),getBoundingBox(),level,java.util.List.of());
            setBoundingBox(getBoundingBox().move(moved));setLocationFromBoundingbox();onGround=my!=moved.y&&my<0;
            if(mx!=moved.x)xd=0;if(my!=moved.y)yd=0;if(mz!=moved.z)zd=0;
        }
        @Override public float getQuadSize(float partial){return super.getQuadSize(partial)*fade();}
        @Override public void render(com.mojang.blaze3d.vertex.VertexConsumer out,net.minecraft.client.Camera camera,float partial){setAlpha(initialAlpha*fade());super.render(out,camera,partial);}
    }
    public static void bolt(ConciliumArcNetwork.Bolt packet,Connection connection){
        if(!connected(packet.dimension(),connection))return;var mc=Minecraft.getInstance();
        if(!(mc.level.getEntity(packet.caster()) instanceof LivingEntity caster))return;
        double distance=caster.position().distanceToSqr(packet.target());if(!Double.isFinite(distance)||distance>65536)return;
        double yaw=Math.toRadians(caster.getYRot());double y=caster==mc.player?caster.getEyeY()-.06:caster.getBoundingBox().minY+caster.getBbHeight()/2+.25;
        var from=new Vec3(caster.getX()-Math.cos(yaw)*.06,y,caster.getZ()-Math.sin(yaw)*.06).add(caster.getLookAngle().scale(.3));
        if(from.distanceToSqr(packet.target())<1e-10)return;
        world(mc.level);if(BOLTS.size()>=2000)BOLTS.remove(0);
        BOLTS.add(new Bolt(new ConciliumLightningGeometry(from,packet.target(),mc.level.random.nextLong()),packet.color()));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        world(mc.level);if(mc.isPaused())return;BUBBLES.forEach(CrucibleBubble::tick);BUBBLES.removeIf(bubble->!bubble.isAlive());
        BOLTS.forEach(bolt->bolt.geometry.tick());BOLTS.removeIf(bolt->bolt.geometry.expired());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||current==null)return;
        var camera=event.getCamera().getPosition();var view=new org.joml.Vector3f(0,0,1).rotate(event.getCamera().rotation());var facing=new Vec3(view.x(),view.y(),view.z());
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var matrix=event.getPoseStack().last().pose();
        var bubbleType=TCRenderTypes.orb(ResourceLocation.parse("thaumcraft:textures/misc/particles.png"));var bubbleOut=buffers.getBuffer(bubbleType);BUBBLES.forEach(bubble->bubble.renderWorld(bubbleOut,event));buffers.endBatch(bubbleType);
        for(int pass=0;pass<2;pass++){
            // Type 10 inherits vanilla EffectRenderer's alpha blend; it never selects additive mode.
            var type=TCRenderTypes.softParticle(TEXTURES[pass]);var out=buffers.getBuffer(type);
            var player=Minecraft.getInstance().player;var observer=player==null?camera:player.position();
            for(var entry:BOLTS){var bolt=entry.geometry;double limit=Minecraft.useFancyGraphics()?100:50;if(bolt.origin.distanceToSqr(observer)>limit*limit)continue;
                float fade=Math.max(0,bolt.age)/(float)bolt.lifetime,alpha=pass==0?(1-fade)*.4f:1-fade*.5f;int visible=bolt.visibleSegments(event.getPartialTick());
                for(var edge:bolt.edges){if(edge.progress>visible)continue;
                    float width=.125f*((float)edge.start().distanceTo(observer)/5+1)*(1+edge.brightness)/2;
                    var left=edge.join(true,facing,width);var right=edge.join(false,facing,width);var from=edge.start().subtract(camera);var to=edge.end().subtract(camera);
                    line(out,matrix,from,to,left,right,.5f,.5f,entry.color,alpha*edge.brightness);
                    if(edge.next==null)line(out,matrix,to,to.add(edge.direction().normalize().scale(width)),right,right,.5f,0,entry.color,alpha*edge.brightness);
                    if(edge.previous==null)line(out,matrix,from.subtract(edge.direction().normalize().scale(width)),from,left,left,0,.5f,entry.color,alpha*edge.brightness);
                }
            }buffers.endBatch(type);
        }
    }
    private static void line(com.mojang.blaze3d.vertex.VertexConsumer out,org.joml.Matrix4f matrix,Vec3 from,Vec3 to,Vec3 left,Vec3 right,float u1,float u2,int color,float alpha){
        vertex(out,matrix,to.subtract(right),u2,0,color,alpha);vertex(out,matrix,from.subtract(left),u1,0,color,alpha);
        vertex(out,matrix,from.add(left),u1,1,color,alpha);vertex(out,matrix,to.add(right),u2,1,color,alpha);
    }
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,org.joml.Matrix4f matrix,Vec3 at,float u,float v,int color,float alpha){out.vertex(matrix,(float)at.x,(float)at.y,(float)at.z).color((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha).uv(u,v).uv2(0xf000f0).endVertex();}
    private ConciliumParticleEffects(){}
}
