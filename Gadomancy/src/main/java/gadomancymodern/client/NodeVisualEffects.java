package gadomancymodern.client;
import gadomancymodern.node.NodeVisualNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class NodeVisualEffects {
    public static class Absorb {public final BlockPos from;public final Vec3 to;public final net.minecraft.world.level.block.state.BlockState state;public int ticks;public Absorb(BlockPos from,Vec3 to,int ticks){this.from=from;this.to=to;this.ticks=ticks;state=Minecraft.getInstance().level.getBlockState(from);}}
    protected static final java.util.List<Absorb> ABSORB=new java.util.ArrayList<>();protected static Object world;
    protected static final java.util.List<Burst> BURSTS=new java.util.ArrayList<>();
    public static class Burst{public final Vec3 at;public final float scale;public int age;public Burst(Vec3 at,float scale){this.at=at;this.scale=scale;}}
    protected static void reset(){var now=Minecraft.getInstance().level;if(now!=world){world=now;ABSORB.clear();BURSTS.clear();}}
    public static void receive(NodeVisualNetwork.Effect packet){reset();var level=Minecraft.getInstance().level;if(level==null)return;switch(packet.kind){case 0->FamiliarVisuals.effectBolt(packet.from,packet.to,packet.type);case 1->{if(ABSORB.size()<512)ABSORB.add(new Absorb(BlockPos.containing(packet.from),packet.to,Math.min(20,packet.type)));}case 2->{if(BURSTS.size()<256)BURSTS.add(new Burst(packet.to,(1+level.random.nextInt(3))*(level.random.nextFloat()*.5f+.5f)*2));}
        case 3->{int[] palette=packet.type==1?new int[]{395606,75128,791894,65791,65932}:new int[]{11929351,15611778,15956768,16738082,16739632,0xff0000};var pos=BlockPos.containing(packet.to);for(int i=0;i<20;i++){int color=palette[level.random.nextInt(palette.length)];tc4revival.client.EldritchWisps.rune(level,pos.getX()+offset(),pos.getY()+offset(),pos.getZ()+offset(),(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,30+level.random.nextInt(10),-.01f-(level.random.nextBoolean()?(level.random.nextBoolean()?.02f:.01f):0));}}
        case 4->{var pos=BlockPos.containing(packet.to);for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)tc4revival.client.ClientFocusEffects.blockSparkle(level,pos.offset(x,y,z),-9999,10);}
        case 5->WispyLineEffects.add(packet.from,packet.to,40,packet.type);}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;reset();if(world==null||Minecraft.getInstance().isPaused())return;BURSTS.forEach(b->b.age++);BURSTS.removeIf(b->b.age>=31);for(var item:ABSORB){CompressorEffects.absorb(item.from,item.to,item.state);item.ticks--;}ABSORB.removeIf(a->a.ticks<=0);}
    protected static float offset(){var r=Minecraft.getInstance().level.random;return r.nextFloat()*(r.nextBoolean()?1:-1)/2;}
    public static void quad(com.mojang.blaze3d.vertex.VertexConsumer out,org.joml.Matrix4f matrix,float size,float u1,float v1,float u2,float v2,float r,float g,float b,float a,int light){out.vertex(matrix,-size,-size,0).color(r,g,b,a).uv(u1,v1).uv2(light).endVertex();out.vertex(matrix,-size,size,0).color(r,g,b,a).uv(u1,v2).uv2(light).endVertex();out.vertex(matrix,size,size,0).color(r,g,b,a).uv(u2,v2).uv2(light).endVertex();out.vertex(matrix,size,-size,0).color(r,g,b,a).uv(u2,v1).uv2(light).endVertex();}
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;reset();var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.runes(net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/misc/nodes.png"));var out=buffers.getBuffer(type);for(var burst:BURSTS){var relative=burst.at.subtract(event.getCamera().getPosition());var pose=event.getPoseStack();pose.pushPose();pose.translate(relative.x,relative.y,relative.z);pose.mulPose(event.getCamera().rotation());quad(out,pose.last().pose(),burst.scale,(burst.age+1)/32f,1,burst.age/32f,31/32f,1,1,1,.75f,240);pose.popPose();}buffers.endBatch(type);}
}
