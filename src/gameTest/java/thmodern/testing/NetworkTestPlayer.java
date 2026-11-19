package thmodern.testing;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Runs vanilla server teleport/menu behavior without opening a client or a network port.
 * Forge FakePlayer's packet listener intentionally makes teleports no-ops and is disconnected.
 */
public final class NetworkTestPlayer extends ServerPlayer {
    public NetworkTestPlayer(ServerLevel level,GameProfile profile){
        super(level.getServer(),level,profile);
        var wire=new Connection(PacketFlow.SERVERBOUND){
            @Override public boolean isConnected(){return true;}
            @Override public void send(Packet<?> packet,PacketSendListener listener){}
        };
        connection=new ServerGamePacketListenerImpl(level.getServer(),wire,this){
            @Override public void send(Packet<?> packet){}
            @Override public void send(Packet<?> packet,PacketSendListener listener){}
        };
    }
}
