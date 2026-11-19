package thmodern.vat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Requests contain only an action; authority always comes from the connection's sender. */
public final class BodyNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumichorizons:body"),()->"1","1"::equals,"1"::equals);
    public record Action(int id){}
    public record State(CompoundTag data){}
    public static void register(){
        CHANNEL.messageBuilder(Action.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->b.writeByte(p.id())).decoder(b->new Action(b.readUnsignedByte()))
            .consumerMainThread((p,ctx)->{var player=ctx.get().getSender();if(player!=null)SelfInfusions.toggle(player,p.id());}).add();
        CHANNEL.messageBuilder(State.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeNbt(p.data())).decoder(b->new State(b.readNbt()))
            .consumerMainThread((p,ctx)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thmodern.client.BodyKeys.receive(p.data()))).add();
    }
    public static void sync(net.minecraft.world.entity.player.Player player){
        if(player instanceof ServerPlayer server&&server.server.getPlayerList().getPlayer(server.getUUID())==server){
            var source=player.getPersistentData().getCompound(SelfInfusions.DATA);var data=SelfInfusions.write(SelfInfusions.read(source));
            data.putBoolean("ClimbDisabled",source.getBoolean("ClimbDisabled"));data.putBoolean("InvisibleDisabled",source.getBoolean("InvisibleDisabled"));
            CHANNEL.send(PacketDistributor.PLAYER.with(()->server),new State(data));
        }
    }
}
