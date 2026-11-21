package tc4revival.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class HoverControls {
    public static KeyMapping TOGGLE;
    private static final ResourceLocation PARTICLES = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static long nextSound;
    private static Object playerIdentity;
    private HoverControls() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (playerIdentity != player) { playerIdentity = player; nextSound = 0; }
        if (player == null) return;
        if (!player.getAbilities().instabuild && !player.isSpectator() && HoverFlight.equipped(player) && player.getAbilities().mayfly)
            player.getAbilities().flying = HoverFlight.active(player);
        if (event.phase == TickEvent.Phase.START) {
            while (TOGGLE.consumeClick()) if (mc.screen == null && HoverFlight.equipped(player))
                tc4revival.network.TCNetwork.CHANNEL.sendToServer(new tc4revival.network.HoverTogglePacket());
            return;
        }
        if (mc.isPaused() || !HoverFlight.equipped(player) || player.getAbilities().instabuild || !player.getAbilities().flying || !HoverFlight.active(player)) return;
        var armor = player.getItemBySlot(EquipmentSlot.CHEST);
        int haste = EnchantmentHelper.getItemEnchantmentLevel(TCEnchantments.HASTE.get(), armor);
        float inertia = HoverFuel.inertia(haste, HoverFlight.girdle(player));
        player.setDeltaMovement(player.getDeltaMovement().multiply(inertia, 1, inertia));
        long now = net.minecraft.Util.getMillis();
        if (now > nextSound) { nextSound = now + 1200; player.playSound(TCSounds.JACOBS.get(), .05f, 1 + player.getRandom().nextFloat() * .05f); }
    }
    public static void render(GuiGraphics graphics, int height) {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || mc.options.hideGui || !HoverFlight.equipped(player)) return;
        var armor = player.getItemBySlot(EquipmentSlot.CHEST); int level = Math.round(HoverFuel.fuel(armor) / 64f * 48);
        graphics.setColor(0, 1, .75f, 1);
        LegacyGui.blit(graphics,PARTICLES, 6, height / 2 + 24 - level, 224, 48 - level, 8, level);
        graphics.setColor(1, 1, 1, 1); LegacyGui.blit(graphics,PARTICLES, 5, height / 2 - 28, 240, 0, 10, 56);
        if (HoverFlight.active(player)) {
            graphics.setColor(1, 1, 1, .66f);
            LegacyGui.blit(graphics,PARTICLES, 2, height / 2 - 43, 16 * (int) (net.minecraft.Util.getMillis() % 700 / 50), 32, 16, 16);
            graphics.setColor(1, 1, 1, 1);
        }
        graphics.renderItem(armor, 2, height / 2 - 43);
    }
}
