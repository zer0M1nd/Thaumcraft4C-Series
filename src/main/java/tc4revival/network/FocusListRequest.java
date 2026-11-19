package tc4revival.network;

import java.util.LinkedHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.content.CastingWandItem;
import tc4revival.content.FocusSelection;

/** Read focus storage on the server, including inventories not replicated in item NBT. */
public record FocusListRequest(int request, InteractionHand hand) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(request); buffer.writeEnum(hand); }
    public static FocusListRequest decode(FriendlyByteBuf buffer) { return new FocusListRequest(buffer.readVarInt(), buffer.readEnum(InteractionHand.class)); }
    public void handle(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu) return;
        var wand = player.getItemInHand(hand);
        if (!(wand.getItem() instanceof CastingWandItem casting) || casting.isSceptre(wand)) return;
        var foci = new LinkedHashMap<String, ItemStack>();
        FocusSelection.available(player).foci().forEach((key, source) -> foci.put(key, source.get().get().copy()));
        TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FocusListReply(request, foci));
    }
}
