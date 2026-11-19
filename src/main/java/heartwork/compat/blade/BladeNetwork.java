package heartwork.compat.blade;

import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BladeNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(BladeCompat.id("blade_controls"),() -> "1","1"::equals,"1"::equals);
    public record ToggleFlux(){}
    public static void register(){
        CHANNEL.messageBuilder(ToggleFlux.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((message,buffer) -> {})
            .decoder(buffer -> new ToggleFlux()).consumerMainThread((message,context) -> {
                var player=context.get().getSender();
                if(player!=null&&!player.isSpectator())FluxBladeItem.toggle(player,player.getMainHandItem());
                context.get().setPacketHandled(true);
            }).add();
    }
    public static void toggle(){CHANNEL.sendToServer(new ToggleFlux());}
}
