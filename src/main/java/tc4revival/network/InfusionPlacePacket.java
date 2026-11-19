package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record InfusionPlacePacket(ResourceLocation recipe,ItemStack center) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeResourceLocation(recipe);buffer.writeItem(center); }
    public static InfusionPlacePacket decode(FriendlyByteBuf buffer) { return new InfusionPlacePacket(buffer.readResourceLocation(),buffer.readItem()); }
    public void handle(ServerPlayer player) { tc4revival.content.InfusionQuickPlacement.place(player,recipe,center); }
}
