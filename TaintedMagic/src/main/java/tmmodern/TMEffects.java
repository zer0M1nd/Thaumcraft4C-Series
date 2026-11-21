package tmmodern;
import net.minecraft.world.level.Level;import net.minecraft.world.phys.Vec3;import net.minecraft.world.entity.Entity;import net.minecraft.resources.ResourceLocation;import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;
public class TMEffects {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("taintedmagic:effects"),()->"1","1"::equals,"1"::equals);
    public record Healing(Vec3 at){}
    public record Sparkles(Vec3 at,int count,int color,float size,float gravity){}
    public static void register(){CHANNEL.messageBuilder(Sparkles.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeDouble(m.at.x);b.writeDouble(m.at.y);b.writeDouble(m.at.z);b.writeVarInt(m.count);b.writeVarInt(m.color);b.writeFloat(m.size);b.writeFloat(m.gravity);}).decoder(b->new Sparkles(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readFloat())).consumerMainThread((m,c)->TMClient.sparkles(m)).add();CHANNEL.messageBuilder(Healing.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeDouble(m.at.x);b.writeDouble(m.at.y);b.writeDouble(m.at.z);}).decoder(b->new Healing(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()))).consumerMainThread((m,c)->TMClient.healing(m.at)).add();}
    public static void sparkles(Level l,Vec3 p,int count,int color,float size,float gravity){if(!l.isClientSide)CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(p.x,p.y,p.z,64,l.dimension())),new Sparkles(p,count,color,size,gravity));}
    public static void healing(Level l,Vec3 p){if(!l.isClientSide)CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(p.x,p.y,p.z,64,l.dimension())),new Healing(p));}
    public static void localSparkles(Level l,Vec3 p,int count,int color,float size,float gravity){if(l.isClientSide)TMClient.sparkles(new Sparkles(p,count,color,size,gravity));}
    public static void lightning(Entity from,Entity to){tc4revival.network.FocusEffectPacket.lightning(from.level(),from,to);sparkles(to.level(),to.position().add(0,to.getBbHeight()/2,0),5,2,2,0);}
}
