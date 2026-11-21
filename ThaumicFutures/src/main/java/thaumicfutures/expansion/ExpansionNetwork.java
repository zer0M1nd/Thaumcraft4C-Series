package thaumicfutures.expansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ExpansionNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicfutures:expansion"),()->"1","1"::equals,"1"::equals);
    public record CloudJump(){}
    public static void register(){
        CHANNEL.messageBuilder(CloudJump.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder((message,buffer)->{}).decoder(buffer->new CloudJump())
            .consumerMainThread((message,context)->{
                var sender=context.get().getSender();if(sender!=null)RestoredAccessories.cloudJump(sender);context.get().setPacketHandled(true);
            }).add();
    }
    private ExpansionNetwork(){}
}
