package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import tc4revival.content.ArchitectFocusItem;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandFocusItem;
import tc4revival.content.WandParts;

public record WandAreaPacket(InteractionHand hand) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeEnum(hand); }
    public static WandAreaPacket decode(FriendlyByteBuf buffer) { return new WandAreaPacket(buffer.readEnum(InteractionHand.class)); }
    public void handle(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        var wand = player.getItemInHand(hand);
        if (wand.getItem() instanceof tc4revival.content.ElementalShovelItem) {
            tc4revival.content.ElementalShovelLayout.orientation(wand,tc4revival.content.ElementalShovelLayout.orientation(wand)+1);
            player.getInventory().setChanged(); player.containerMenu.broadcastChanges(); return;
        }
        if (!(wand.getItem() instanceof CastingWandItem casting) || casting.isSceptre(wand)) return;
        var focus = WandFocusItem.attached(wand);
        if (focus.getItem() instanceof ArchitectFocusItem architect) { architect.toggle(wand, player.isShiftKeyDown()); player.getInventory().setChanged(); player.containerMenu.broadcastChanges(); }
    }
}
