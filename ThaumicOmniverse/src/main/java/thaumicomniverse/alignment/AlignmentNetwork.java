package thaumicomniverse.alignment;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AlignmentNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicomniverse:alignment"),()->"1","1"::equals,"1"::equals);
    public record View(int menu,CompoundTag tag) {}
    public static void register() {
        CHANNEL.registerMessage(0,View.class,(p,b)->{b.writeInt(p.menu());b.writeNbt(p.tag());},b->new View(b.readInt(),b.readNbt()),(p,context)->{
            var ctx=context.get();ctx.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumicomniverse.client.AlignmentScreen.receive(p)));ctx.setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayer player,int menu,CompoundTag tag) {CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new View(menu,tag));}
}
