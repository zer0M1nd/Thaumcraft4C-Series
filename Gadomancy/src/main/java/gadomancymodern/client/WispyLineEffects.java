package gadomancymodern.client;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Original drawFloatyLine wave and crossing ribbons; the dispatcher lifetime was measured in render frames. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class WispyLineEffects {
    public static class Line{public final Vec3 from,to;public final int color;public int frames;public Line(Vec3 from,Vec3 to,int frames,int color){this.from=from;this.to=to;this.frames=frames;this.color=color;}}
    protected static final java.util.List<Line> LINES=new java.util.ArrayList<>();protected static Object world;
    protected static void reset(){var now=Minecraft.getInstance().level;if(now!=world){world=now;LINES.clear();}}
    public static void add(Vec3 from,Vec3 to,int frames,int color){reset();if(LINES.size()<2048)LINES.add(new Line(from,to,frames,color));}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;reset();var mc=Minecraft.getInstance();if(mc.level==null)return;var buffers=mc.renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/misc/wispy.png"));var out=buffers.getBuffer(type);float time=System.nanoTime()/30000000L,quality=tc4revival.config.TCConfig.CLIENT.golemLinkQuality()/2f;for(var line:LINES){var delta=line.from.subtract(line.to);float distance=(float)delta.length(),segments=Math.round(distance)*quality;if(segments<1){line.frames--;continue;}for(int plane=0;plane<2;plane++){var width=plane==0?new Vec3(0,.15,0):new Vec3(.15,0,0);for(int i=0;i<(int)segments;i++){var a=point(line,i,segments,delta,distance,quality,time).subtract(event.getCamera().getPosition());var b=point(line,i+1,segments,delta,distance,quality,time).subtract(event.getCamera().getPosition());float p=i/segments,q=(i+1)/segments,alphaA=1-Math.abs(i-segments/2)/(segments/2),alphaB=1-Math.abs(i+1-segments/2)/(segments/2);vertex(out,event,a.subtract(width),(1-p)*distance+time*.02f,1,line.color,alphaA);vertex(out,event,a.add(width),(1-p)*distance+time*.02f,0,line.color,alphaA);vertex(out,event,b.add(width),(1-q)*distance+time*.02f,0,line.color,alphaB);vertex(out,event,b.subtract(width),(1-q)*distance+time*.02f,1,line.color,alphaB);}}line.frames--;}LINES.removeIf(l->l.frames<=0);buffers.endBatch(type);}
    protected static Vec3 point(Line line,int i,float segments,Vec3 delta,float distance,float quality,float time){float p=i/segments,alpha=1-Math.abs(i-segments/2)/(segments/2);double wave=distance*(1-p)*quality-time%32767/5;return line.to.add(delta.add(Math.sin((line.from.z%16+wave)/4)*.5*alpha,Math.sin((line.from.x%16+wave)/3)*.5*alpha,Math.sin((line.from.y%16+wave)/2)*.5*alpha).scale(p));}
    protected static void vertex(VertexConsumer out,RenderLevelStageEvent e,Vec3 p,float u,float v,int rgb,float a){out.vertex(e.getPoseStack().last().pose(),(float)p.x,(float)p.y,(float)p.z).color((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,a).uv(u,v).uv2(220).endVertex();}
}
