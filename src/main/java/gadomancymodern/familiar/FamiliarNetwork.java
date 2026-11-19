package gadomancymodern.familiar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public class FamiliarNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("gadomancy","familiar"),()->"1","1"::equals,"1"::equals);
    public static class Bolt {public final int owner,type;public final Vec3 target;public Bolt(int owner,Vec3 target,int type){this.owner=owner;this.target=target;this.type=type;}}
    public static void register(){CHANNEL.messageBuilder(Bolt.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.owner);b.writeDouble(p.target.x);b.writeDouble(p.target.y);b.writeDouble(p.target.z);b.writeVarInt(p.type);}).decoder(b->new Bolt(b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt())).consumerMainThread((p,c)->{gadomancymodern.client.FamiliarVisuals.bolt(p);c.get().setPacketHandled(true);}).add();}
    public static void bolt(Player owner,Vec3 target,int type){CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(owner.getX(),owner.getY(),owner.getZ(),16,owner.level().dimension())),new Bolt(owner.getId(),target,type));}
}
