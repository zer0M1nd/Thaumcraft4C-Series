package tc4revival.client;

import java.util.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.network.LegacyEffectPacket;

/** TC4 atlas animation, additive wisps/sparkles, opaque shadow trails and rotating beam ribbons. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class LegacyAddonEffects {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png"),BEAM=ResourceLocation.parse("thaumcraft:textures/misc/beam.png");
    private static final List<Mote> MOTES=new ArrayList<>();
    private static ClientLevel current;
    private static class Mote {
        LegacyEffectPacket data;Vec3 at,previous,velocity=Vec3.ZERO;int age;boolean landed;float size,startAngle;
        Mote(LegacyEffectPacket data,Vec3 at,float random,float angle) {this.data=data;this.at=at;previous=at;size=data.scale()*(data.style().contains("sparkle")||data.style().contains("wisp")||data.style().equals("shadow")?1+random:.5f+random*.5f);startAngle=angle;if(data.style().endsWith("_path"))velocity=data.to().subtract(at).scale(1d/data.life());}
        void tick() {previous=at;age++;
            if(data.style().equals("xp_drop")){
                if(landed)return;velocity=velocity.add(0,-.06,0);if(age<=40)velocity=velocity.scale(.02);var next=at.add(velocity);velocity=velocity.scale(.98);
                var pos=net.minecraft.core.BlockPos.containing(next);var state=current.getBlockState(pos);var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());boolean jar=id!=null&&id.getPath().contains("jar");
                if(jar&&next.y<=pos.getY()+.15){next=new Vec3(next.x,pos.getY()+.15,next.z);landed=true;}
                else if(!jar&&!state.getCollisionShape(current,pos).isEmpty()){next=new Vec3(next.x,Math.max(at.y,pos.getY()+1.01),next.z);landed=true;}
                at=next;return;
            }
if(!data.style().equals("beam")&&!data.style().equals("spiral")){velocity=velocity.add(0,-.04*data.gravity(),0);at=at.add(velocity);if(!data.style().equals("shadow"))velocity=velocity.scale(data.style().contains("wisp")?.98:.908);}}
    }
    public static void receive(LegacyEffectPacket packet,net.minecraft.network.Connection connection) {
        var mc=Minecraft.getInstance();if(mc.getConnection()!=null&&mc.getConnection().getConnection()==connection&&connection.isConnected())emit(packet);
    }
    public static void emit(LegacyEffectPacket packet) {
        var mc=Minecraft.getInstance();if(mc.level==null||!packet.valid()||!mc.level.dimension().location().equals(packet.dimension()))return;
        if(packet.style().equals("slimy_bubble")){var p=packet.from();AlchemyBubbleParticles.colored(mc.level,p.x,p.y,p.z,packet.scale(),packet.color());return;}
        if(packet.style().equals("rune")){var p=packet.from();for(int i=0;i<packet.count();i++)EldritchWisps.rune(mc.level,p.x,p.y,p.z,(packet.color()>>16&255)/255f,(packet.color()>>8&255)/255f,(packet.color()&255)/255f,packet.life(),packet.gravity());return;}
        if(packet.style().equals("essentia")||packet.style().equals("infusion_item")){InfusionIntakeEffects.external(packet.from(),packet.to(),packet.color(),packet.scale(),packet.style().equals("infusion_item"));return;}
        if(packet.style().startsWith("wand:")) {
            var parts=packet.style().split(":");if(parts.length!=4)return;
            try {int type=Integer.parseInt(parts[1]),id=Integer.parseInt(parts[2]);if(type<0||type>3)return;var actor=mc.level.getEntity(id);
                if(actor instanceof net.minecraft.world.entity.LivingEntity player&&player.position().distanceToSqr(packet.from())<64)ContinuousFocusBeam.update(player,packet.to(),type,packet.color(),packet.scale(),parts[3].equals("1"));
            }catch(NumberFormatException ignored){}return;
        }
        if(!Set.of("sparkle","dark_sparkle","sparkle_path","wisp","wisp_path","dark_wisp_path","glow","dark_glow","dark_wisp","shadow","spiral","beam","wispy","containment","sonic","forge_ring","xp_drop").contains(packet.style()))return;
        if(current!=mc.level){MOTES.clear();current=mc.level;}
        if(mc.getCameraEntity()==null||mc.getCameraEntity().position().distanceToSqr(packet.from())>4096)return;
        int divisor=switch(mc.options.particles().get()){case ALL->1;case DECREASED->2;case MINIMAL->4;};
        var random=mc.level.random;
        for(int i=0;i<packet.count();i++) {
            if(i%divisor!=0)continue;
            var at=packet.from().add((random.nextDouble()-.5)*2*packet.spread(),(random.nextDouble()-.5)*2*packet.spread(),(random.nextDouble()-.5)*2*packet.spread());
            if(MOTES.size()>=2048)MOTES.remove(0);MOTES.add(new Mote(packet,at,random.nextFloat(),random.nextFloat()*360));
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){MOTES.clear();current=null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();if(event.phase!=TickEvent.Phase.START)return;
        if(current!=mc.level){MOTES.clear();current=mc.level;}if(mc.isPaused())return;
        MOTES.forEach(Mote::tick);MOTES.removeIf(p->p.age>=p.data.life());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||current==null||current!=mc.level)return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        for(String group:List.of("beam","sparkle","dark_sparkle","sparkle_path","wisp","wisp_path","dark_wisp_path","glow","dark_glow","dark_wisp","shadow","spiral","wispy","containment","sonic","forge_ring","xp_hang","xp_fall","xp_land")) {
        var renderType=renderType(group);

        for(var mote:MOTES) {
            if(!groupOf(mote).equals(group))continue;
            var d=mote.data;var style=d.style();float time=mote.age+event.getPartialTick(),progress=time/d.life();
            if(style.equals("beam")||style.equals("wispy")) {
                var from=d.from().subtract(camera);var to=d.to().subtract(camera);var delta=to.subtract(from);if(delta.lengthSqr()<1e-8)continue;
                var along=delta.normalize();var first=along.cross(Math.abs(along.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();var second=along.cross(first).normalize();
                var type=renderType;var out=TCRenderTypes.buffer(buffers,type);
                double growth=style.equals("beam")?Math.min(time/4,d.scale())/d.scale():Math.min(1,time/4),rotation=(current.getGameTime()%72+event.getPartialTick())*Math.PI/36;
                float alpha=(style.equals("beam")?.4f:.5f)*Math.min(1,(d.life()-time)/4);float clock=mc.player==null?time:mc.player.tickCount+event.getPartialTick();float scroll=-clock*.2f-(float)Math.floor(-clock*.1f);
                for(int i=0;i<3;i++){double angle=rotation+i*Math.PI/3;var width=first.scale(Math.cos(angle)).add(second.scale(Math.sin(angle))).scale(d.scale()*.15*growth);var tip=from.add(delta.scale(growth));float phase=scroll+i/3f-1;
                    vertex(out,pose.last().pose(),from.subtract(width),1,phase,d.color(),alpha);vertex(out,pose.last().pose(),from.add(width),0,phase,d.color(),alpha);
                    vertex(out,pose.last().pose(),tip.add(width),0,phase+(float)delta.length(),d.color(),alpha);vertex(out,pose.last().pose(),tip.subtract(width),1,phase+(float)delta.length(),d.color(),alpha);}
                continue;
            }
            if(style.equals("sonic")){
                var relative=d.from().subtract(camera);var direction=d.to().subtract(d.from()).normalize();var frame=Math.min(15,1+(int)(14*progress));
                var type=TCRenderTypes.infusionRunes(ResourceLocation.parse("thaumcraft:textures/models/ripple"+frame+".png"));
                pose.pushPose();pose.translate(relative.x,relative.y,relative.z);pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,0,1),new org.joml.Vector3f((float)direction.x,(float)direction.y,(float)direction.z)));pose.scale(.5f,.5f,-.5f);
                EldritchMeshes.drawTint("hemis","GeoSphere001",pose,buffers,type,0xf000f0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0,.5f,.5f,1);pose.popPose();TCRenderTypes.endBatch(buffers,type);continue;
            }
            var at=mote.previous.lerp(mote.at,event.getPartialTick());float u=(mote.age/Math.max(1,d.life()/3)%4)/16f,v=.25f,step=.0624375f,half=.1f*mote.size*(1-progress),alpha=1;
            if(style.equals("wisp")||style.equals("wisp_path")||style.equals("dark_wisp_path")||style.equals("dark_wisp")){u=0;v=.875f;step=.125f;half=.5f*mote.size*Math.max(0,1-Math.abs(2*progress-1));alpha=.5f;}
            if(style.equals("glow")||style.equals("dark_glow")){u=0;v=.875f;step=.125f;half=.5f*mote.size;alpha=.5f;}
            if(style.equals("shadow")){u=(1+mote.age)%4/16f;half=.1f*mote.size;}
            if(style.equals("spiral")){double az=Math.toRadians(mote.startAngle+720*progress),polar=Math.toRadians(90-180*progress),radius=d.to().x;at=d.from().add(-Math.sin(az)*Math.cos(polar)*radius,-Math.sin(polar)*radius,Math.cos(az)*Math.cos(polar)*radius);at=new Vec3(at.x,Math.max(d.to().y+.1,at.y),at.z);u=(1+(int)(4*progress))/16f;v=0;half=.15f*mote.size;alpha=.66f*(1-progress);}
            if(style.equals("containment")){u=(current.getGameTime()%16)/16f;v=0;step=.0625f;half=3*d.scale()*(float)Math.pow(1.15,-time);alpha=.1f;}
            float vEnd=v+step;
            if(style.equals("forge_ring")){u=((int)((current.getGameTime()+event.getPartialTick())*1.25f)%16)/16f;step=.0625f;v=0;vEnd=1;half=.2f*d.scale();alpha=.9f;}
            if(style.equals("xp_drop")){u=0;v=0;step=1;vEnd=1;half=.05f*mote.size;}
            var type=renderType;var relative=at.subtract(camera);
            pose.pushPose();pose.translate(relative.x,relative.y,relative.z);pose.mulPose(event.getCamera().rotation());
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,type),pose.last().pose(),half,u,v,u+step,style.equals("containment")?1:vEnd,(d.color()>>16&255)/255f,(d.color()>>8&255)/255f,(d.color()&255)/255f,alpha,0xf000f0);
            pose.popPose();
        }
        TCRenderTypes.endBatch(buffers,renderType);
        }
    }
    private static String groupOf(Mote mote){return mote.data.style().equals("xp_drop")?(mote.landed?"xp_land":mote.age<=40?"xp_hang":"xp_fall"):mote.data.style();}
    private static net.minecraft.client.renderer.RenderType renderType(String style){
        if(style.equals("beam")||style.equals("wispy"))return TCRenderTypes.runes(style.equals("beam")?BEAM:ResourceLocation.parse("thaumcraft:textures/misc/wispy.png"));
        String texture=switch(style){case "containment"->"thaumcraft:textures/misc/eldritch_portal.png";case "forge_ring"->"thaumcraft:textures/items/lightningringv.png";case "xp_hang"->"minecraft:textures/particle/drip_hang.png";case "xp_fall"->"minecraft:textures/particle/drip_fall.png";case "xp_land"->"minecraft:textures/particle/drip_land.png";default->null;};
        return texture!=null?TCRenderTypes.softParticle(ResourceLocation.parse(texture)):style.equals("shadow")||style.equals("spiral")||style.startsWith("dark_")?TCRenderTypes.softParticle(ATLAS):TCRenderTypes.runes(ATLAS);
    }
    private static void vertex(VertexConsumer out,org.joml.Matrix4f pose,Vec3 p,float u,float v,int color,float alpha){out.vertex(pose,(float)p.x,(float)p.y,(float)p.z).color((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha).uv(u,v).uv2(0xf000f0).endVertex();}
}
