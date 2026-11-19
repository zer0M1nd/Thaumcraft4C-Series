package tc4revival.network;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.FocalManipulatorMenu;

public record FocalBatchPacket(int container, ItemStack expected, List<Integer> choices) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(container); buffer.writeItem(expected);
        buffer.writeCollection(choices, FriendlyByteBuf::writeVarInt);
    }
    public static FocalBatchPacket decode(FriendlyByteBuf buffer) {
        return new FocalBatchPacket(buffer.readVarInt(), buffer.readItem(),
                buffer.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, 5), FriendlyByteBuf::readVarInt));
    }
    public void handle(ServerPlayer player) {
        if (player != null && player.containerMenu instanceof FocalManipulatorMenu menu && menu.containerId == container)
            menu.startBatch(player, expected, choices);
    }
}
