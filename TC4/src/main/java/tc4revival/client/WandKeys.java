package tc4revival.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandParts;
import tc4revival.network.FocusChangePacket;
import tc4revival.network.TCNetwork;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class WandKeys {
    public static KeyMapping CHANGE_FOCUS;
    public static KeyMapping CHANGE_AREA;
    private WandKeys() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || mc.screen != null) return;
        var bellHand=player.getMainHandItem().getItem() instanceof tc4revival.content.GolemBellItem?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        if (player.getItemInHand(bellHand).getItem() instanceof tc4revival.content.GolemBellItem) {
            if (CHANGE_FOCUS.isDown() && CHANGE_FOCUS.getKey().equals(mc.options.keySwapOffhand.getKey()))
                while (mc.options.keySwapOffhand.consumeClick()) {}
            while (CHANGE_FOCUS.consumeClick()) TCNetwork.CHANNEL.sendToServer(new tc4revival.network.GolemBellResetPacket(bellHand));
        }
        var shovelHand=player.getMainHandItem().getItem() instanceof tc4revival.content.ElementalShovelItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (player.getItemInHand(shovelHand).getItem() instanceof tc4revival.content.ElementalShovelItem) {
            while (CHANGE_AREA.consumeClick()) TCNetwork.CHANNEL.sendToServer(new tc4revival.network.WandAreaPacket(shovelHand));
        }
        var hand = player.getMainHandItem().getItem() instanceof CastingWandItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var wand = player.getItemInHand(hand);
        if (!(wand.getItem() instanceof CastingWandItem casting) || casting.isSceptre(wand)) return;
        while (CHANGE_AREA.consumeClick()) TCNetwork.CHANNEL.sendToServer(new tc4revival.network.WandAreaPacket(hand));
        if (CHANGE_FOCUS.isDown() && CHANGE_FOCUS.getKey().equals(mc.options.keySwapOffhand.getKey()))
            while (mc.options.keySwapOffhand.consumeClick()) {} // A user-bound focus key takes precedence while holding a wand.
        while (CHANGE_FOCUS.consumeClick()) {
            if (player.isShiftKeyDown()) TCNetwork.CHANNEL.sendToServer(new FocusChangePacket(hand, "REMOVE"));
            else mc.setScreen(new FocusRadialScreen(hand, wand));
        }
    }
}
