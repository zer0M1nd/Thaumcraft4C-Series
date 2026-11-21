package emtmodern.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public class QuantumChestNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("emt","quantum_chest"),()->"1","1"::equals,"1"::equals);
    public record Detach() {}
    public static void register(){CHANNEL.messageBuilder(Detach.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((message,buffer)->{}).decoder(buffer->new Detach()).consumerMainThread((message,context)->{
        var player=context.get().getSender();if(player!=null){var stack=player.getItemBySlot(EquipmentSlot.CHEST);if(stack.getItem() instanceof InfusedQuantumChestplate&&player.isShiftKeyDown()&&player.getMainHandItem().isEmpty())stack.getOrCreateTag().putBoolean("unequip",true);}
        context.get().setPacketHandled(true);
    }).add();}
}
