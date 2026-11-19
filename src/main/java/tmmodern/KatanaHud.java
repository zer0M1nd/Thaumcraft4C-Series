package tmmodern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;
import tc4revival.client.LegacyGui;

/** Two translucent layers, including the old breathing rune size and equip fade. */
public class KatanaHud {
    private static final ResourceLocation SCRIPT = ResourceLocation.parse("thaumcraft:textures/misc/script.png");
    private static final KatanaHudState STATE = new KatanaHudState();
    private static ClientLevel world;
    private static ItemStack last = ItemStack.EMPTY;

    public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (world != mc.level) { world = mc.level; last = ItemStack.EMPTY; STATE.reset(); }
        if (mc.player == null) return;
        var held = mc.player.getMainHandItem();
        boolean equipped = held.getItem() instanceof KatanaItem;
        if (equipped) last = held.copy();
        float fade = STATE.advance(equipped, mc.isPaused() ? 0 : event.getPartialTick());
        if (last.isEmpty() || fade == 0) return;
        int cooldown = last.hasTag() ? last.getTag().getInt("cooldown") : 0;
        float fill = Math.min(1, cooldown > 0 ? cooldown / 140f : mc.player.getTicksUsingItem() / 20f);
        var g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().scale(.315f, .315f, .315f);
        float x = g.guiWidth() / 2f / .315f + 30;
        float y = g.guiHeight() / .315f - (mc.player.isCreative() ? 100 : 150);
        int ticks = mc.player.tickCount;
        for (int rune = 0; rune < 16; rune++) {
            float pulse = KatanaHudState.pulse(ticks, rune);
            float red = (float) Math.sin((ticks + rune * 5) / 5f) * .1f + .8f;
            float green = (float) Math.sin((ticks + rune * 5) / 7f) * .1f + .7f;
            g.setColor(red, green, .4f, KatanaHudState.backgroundAlpha(ticks, rune, fade));
            draw(g, rune, x, y, pulse);
            if (Math.ceil(16 * fill) > rune) {
                int color = java.awt.Color.HSBtoRGB((cooldown > 0 ? 16 - rune : rune) / 16f * .3f, 1, 1);
                g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f,
                    (color & 255) / 255f, KatanaHudState.progressAlpha(ticks, rune));
                draw(g, rune, x, y, pulse);
            }
        }
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    private static void draw(GuiGraphics g, int rune, float x, float y, float pulse) {
        g.pose().pushPose();
        g.pose().translate(x + rune * 16 - pulse, y - pulse, 0);
        float size = (16 + 2 * pulse) / 16;
        g.pose().scale(size, size, 1);
        LegacyGui.blit(g, SCRIPT, 0, 0, 16, 16, rune * 16, 0, 16, 16, 256, 16);
        g.pose().popPose();
    }
}
