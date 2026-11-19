package gadomancymodern.client;

import com.mojang.blaze3d.vertex.*;
import gadomancymodern.familiar.FamiliarNetwork;
import gadomancymodern.item.EtherealFamiliarItem;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.TCRenderTypes;
import top.theillusivec4.curios.api.CuriosApi;

/** The original double billboard, 160-tick orbit, and seven-pass fractal attack bolts. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class FamiliarVisuals {
    public static final ResourceLocation WISP=new ResourceLocation("thaumcraft","textures/misc/wisp.png"),PARTICLES=new ResourceLocation("thaumcraft","textures/misc/particles.png");
    public static final ResourceLocation[] LIGHTNING={new ResourceLocation("thaumcraft","textures/misc/p_large.png"),new ResourceLocation("thaumcraft","textures/misc/p_small.png")};
    protected static final Map<UUID,Integer> AGES=new HashMap<>();
    protected static final Map<FamiliarBoltGeometry,Integer> BOLTS=new LinkedHashMap<>();
    protected static Object world;
    public static int ticks;
    protected static void reset(){var now=Minecraft.getInstance().level;if(world!=now){world=now;AGES.clear();BOLTS.clear();ticks=0;}}
    public static ItemStack worn(Player player){return CuriosApi.getCuriosInventory(player).resolve().flatMap(i->i.getStacksHandler("necklace")).map(h->{for(int i=0;i<h.getStacks().getSlots();i++){var stack=h.getStacks().getStackInSlot(i);if(stack.getItem() instanceof EtherealFamiliarItem)return stack;}return ItemStack.EMPTY;}).orElse(ItemStack.EMPTY);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;reset();var mc=Minecraft.getInstance();if(mc.level==null||mc.isPaused())return;ticks++;var live=new HashSet<UUID>();
        for(var player:mc.level.players())if(!worn(player).isEmpty()){live.add(player.getUUID());AGES.merge(player.getUUID(),1,Integer::sum);}AGES.keySet().retainAll(live);
        BOLTS.keySet().forEach(FamiliarBoltGeometry::tick);BOLTS.keySet().removeIf(FamiliarBoltGeometry::expired);
    }
    public static Vec3 orbit(int age){double theta=Math.PI*2*age/160.0;return new Vec3(Math.cos(theta),Math.sin(theta*2)/2,Math.sin(theta));}
    public static void nodeBolt(Vec3 from,Vec3 to){reset();var mc=Minecraft.getInstance();if(mc.level!=null)BOLTS.put(new FamiliarBoltGeometry(from,to,mc.level.random.nextLong(),10,4,5),0);}
    public static void effectBolt(Vec3 from,Vec3 to,int type){reset();var mc=Minecraft.getInstance();if(mc.level!=null)BOLTS.put(new FamiliarBoltGeometry(from,to,mc.level.random.nextLong(),10,4,5),type);}
    public static void blackBolt(Vec3 from,Vec3 to){reset();var mc=Minecraft.getInstance();if(mc.level!=null)BOLTS.put(new FamiliarBoltGeometry(from,to,mc.level.random.nextLong(),10,4,5),5);}
    public static void bolt(FamiliarNetwork.Bolt packet){
        reset();var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!(mc.level.getEntity(packet.owner) instanceof Player player)||player.distanceToSqr(mc.player)>gadomancymodern.ClientConfig.limitSquared(1024)||!AGES.containsKey(player.getUUID()))return;
        var offset=orbit(AGES.get(player.getUUID()));var from=player.position().add(offset).add(0,player==mc.player?1.22:1.42,0);BOLTS.put(new FamiliarBoltGeometry(from,packet.target,mc.level.random.nextLong(),10,4,5),packet.type);
    }
    @SubscribeEvent public static void player(RenderPlayerEvent.Post event){
        reset();var player=event.getEntity();var stack=worn(player);if(stack.isEmpty())return;var aspect=tc4revival.aspect.TCAspects.catalog().find(EtherealFamiliarItem.aspect(stack));if(aspect.isEmpty())return;
        int age=AGES.getOrDefault(player.getUUID(),0);var offset=orbit(age);var pose=event.getPoseStack();pose.pushPose();pose.translate(offset.x,offset.y+(player==Minecraft.getInstance().player?.95:2.27),offset.z);pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        int color=aspect.get().color();if(privileged(player)){color=java.awt.Color.HSBtoRGB((System.currentTimeMillis()%3240L)/3240f,1,1);if(player.getRandom().nextInt(3)==0){var r=player.getRandom();tc4revival.client.ClientFocusEffects.wispMote(player.level(),player.position().add(offset).add((r.nextFloat()-r.nextFloat())*.3,1.42+(r.nextFloat()-r.nextFloat())*.3,(r.nextFloat()-r.nextFloat())*.3),color);}}
        wisp(pose,event.getMultiBufferSource(),age,event.getPartialTick(),color);pose.popPose();
    }
    public static boolean privileged(Player player){return Set.of(914342508,1529485240,-1899266570,-335313669,126020810).contains(player.getUUID().hashCode());}
    public static void wisp(PoseStack pose,MultiBufferSource buffers,int age,float partial,int color){
        int frame=Math.floorMod(age,16);float u=frame%4/4f,v=frame/4/4f;quad(buffers.getBuffer(TCRenderTypes.softParticles(WISP)),pose.last().pose(),1,u+.25f-.0000390625f,v,u,v+.25f-.0000390625f,color);
        quad(buffers.getBuffer(TCRenderTypes.softParticles(PARTICLES)),pose.last().pose(),.4f+(float)Math.sin((age+partial)/10)*.1f,(frame+1)/16f,5/16f,frame/16f,6/16f,0xffffff);
    }
    public static void quad(VertexConsumer out,org.joml.Matrix4f matrix,float half,float u1,float v1,float u2,float v2,int color){
        float r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;
        out.vertex(matrix,-half,-half,0).color(r,g,b,1).uv(u2,v2).uv2(240).endVertex();out.vertex(matrix,-half,half,0).color(r,g,b,1).uv(u2,v1).uv2(240).endVertex();out.vertex(matrix,half,half,0).color(r,g,b,1).uv(u1,v1).uv2(240).endVertex();out.vertex(matrix,half,-half,0).color(r,g,b,1).uv(u1,v2).uv2(240).endVertex();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;reset();if(BOLTS.isEmpty())return;var mc=Minecraft.getInstance();var camera=event.getCamera().getPosition();var view=new org.joml.Vector3f(0,0,1).rotate(event.getCamera().rotation());var facing=new Vec3(view.x(),view.y(),view.z());var buffers=mc.renderBuffers().bufferSource();
        for(int pass=0;pass<2;pass++)for(int type:new int[]{0,1,2,3,4,5,6}){var render=type>=5?TCRenderTypes.sharpTranslucent(LIGHTNING[pass]):TCRenderTypes.runes(LIGHTNING[pass]);var out=buffers.getBuffer(render);float[] rgb=color(type,pass);
            for(var entry:BOLTS.entrySet()){if(entry.getValue()!=type)continue;var bolt=entry.getKey();if(bolt.origin.distanceToSqr(camera)>gadomancymodern.ClientConfig.distanceSquared())continue;float fade=Math.max(0,bolt.age)/(float)bolt.lifetime,alpha=pass==0?(1-fade)*.4f:1-fade*.5f;int visible=bolt.visibleSegments(event.getPartialTick());
                for(var edge:bolt.edges){if(edge.progress>visible)continue;float width=.03f*((float)edge.start().distanceTo(camera)/5+1)*(1+edge.brightness)/2;var left=edge.join(true,facing,width);var right=edge.join(false,facing,width);var from=edge.start().subtract(camera);var to=edge.end().subtract(camera);line(out,event.getPoseStack().last().pose(),from,to,left,right,.5f,.5f,alpha*edge.brightness,rgb);if(edge.next==null)line(out,event.getPoseStack().last().pose(),to,to.add(edge.direction().normalize().scale(width)),right,right,.5f,0,alpha*edge.brightness,rgb);if(edge.previous==null)line(out,event.getPoseStack().last().pose(),from.subtract(edge.direction().normalize().scale(width)),from,left,left,0,.5f,alpha*edge.brightness,rgb);}
            }buffers.endBatch(render);
        }
    }
    public static float[] color(int type,int pass){return switch(type){case 0->new float[]{pass==0?.6f:1,pass==0?.3f:.6f,pass==0?.6f:1};case 1->new float[]{pass==0?.6f:1,pass==0?.6f:1,.1f};case 2->new float[]{.1f,.1f,pass==0?.6f:1};case 3->new float[]{.1f,pass==0?1:.6f,.1f};case 4->new float[]{pass==0?.6f:1,.1f,.1f};case 5->pass==0?new float[]{.6f,.2f,.6f}:new float[]{0,0,0};default->new float[]{.75f,1,1};};}
    public static void line(VertexConsumer out,org.joml.Matrix4f pose,Vec3 from,Vec3 to,Vec3 left,Vec3 right,float u1,float u2,float alpha,float[] rgb){vertex(out,pose,to.subtract(right),u2,0,alpha,rgb);vertex(out,pose,from.subtract(left),u1,0,alpha,rgb);vertex(out,pose,from.add(left),u1,1,alpha,rgb);vertex(out,pose,to.add(right),u2,1,alpha,rgb);}
    public static void vertex(VertexConsumer out,org.joml.Matrix4f matrix,Vec3 at,float u,float v,float alpha,float[] rgb){out.vertex(matrix,(float)at.x,(float)at.y,(float)at.z).color(rgb[0],rgb[1],rgb[2],alpha).uv(u,v).uv2(0xf000f0).endVertex();}
}
