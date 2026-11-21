package tc4revival.client;

import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandCooldowns;
import tc4revival.content.WandFocusItem;

/** The six rotating vis gauges use the original HUD atlas and one-second comparison interval. */
public  class WandHud {
    private static final ResourceLocation HUD = ResourceLocation.parse("thaumcraft:textures/gui/hud.png");
    private static final Map<Integer, int[]> PREVIOUS = new HashMap<>();
    private static java.util.List<String> visibleTags=java.util.List.of();
    private static long nextSync;
    private static Object connection;
    private WandHud() {}
    public static void render(GuiGraphics graphics, int height) {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (connection != mc.getConnection()) { connection = mc.getConnection(); PREVIOUS.clear(); nextSync = 0; }
        if (player == null || mc.screen != null && !(mc.screen instanceof FocusRadialScreen) || mc.options.hideGui) return;
        if ((player.getMainHandItem().is(tc4revival.content.TCItems.SANITY_CHECKER.get()) || player.getOffhandItem().is(tc4revival.content.TCItems.SANITY_CHECKER.get()))) return;
        var stack = player.getMainHandItem(); int slot = player.getInventory().selected;
        if (!(stack.getItem() instanceof CastingWandItem)) { stack = player.getOffhandItem(); slot = 40; }
        if (!(stack.getItem() instanceof CastingWandItem wand)) return;
        var primals = thaumcraft.api.client.ClientAspectViews.primals();
        var tags=primals.stream().map(tc4revival.aspect.Aspect::tag).toList();
        if(!visibleTags.equals(tags)){visibleTags=tags;PREVIOUS.clear();nextSync=0;}
        var amounts = new int[primals.size()];
        for (int i = 0; i < amounts.length; i++) amounts[i] = wand.vis(stack, primals.get(i));
        long now = System.currentTimeMillis();
        if (!PREVIOUS.containsKey(slot)) PREVIOUS.put(slot, amounts.clone());
        else if (now >= nextSync) { PREVIOUS.put(slot, amounts.clone()); nextSync = now + 1000; }
        var previous = PREVIOUS.get(slot);var preview=thaumcraft.api.wands.WandCasting.preview(player,stack);
        var focus=preview.focus();var costs=preview.costs();
        boolean bottom = TCConfig.CLIENT.wandDialBottom.get(); var pose = graphics.pose();
        pose.pushPose(); pose.translate(0, bottom ? height - 32 : 0, 0);
        pose.pushPose(); pose.scale(.5f, .5f, 1); LegacyGui.blit(graphics,HUD, 0, 0, 0, 0, 64, 64); pose.popPose();
        pose.translate(16, 16, 0);
        for (int i = 0; i < primals.size(); i++) {
            var aspect = primals.get(i); int amount = amounts[i];
            pose.pushPose();
            if (!bottom) pose.mulPose(Axis.ZP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-15 + i * 24)); pose.translate(0, -32, 0); pose.scale(.5f, .5f, 1);
            int fill = (int) (30f * amount / wand.capacity(stack)), color = aspect.color();
            graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, .8f);
            LegacyGui.blit(graphics,HUD, -4, 35 - fill, 104, 0, 8, fill); graphics.setColor(1, 1, 1, 1);
            LegacyGui.blit(graphics,HUD, -8, -3, 72, 0, 16, 42);
            int offset = 0;
            if (costs.getOrDefault(aspect, 0) > 0) { LegacyGui.blit(graphics,HUD, -4, -8, 136, 0, 8, 8); offset = 8; }
            if (previous[i] != amount) LegacyGui.blit(graphics,HUD, -4, -8 - offset, previous[i] > amount ? 128 : 120, 0, 8, 8);
            if (player.isShiftKeyDown()) {
                pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(-90));
                graphics.drawString(mc.font, Integer.toString(amount / 100), -32, -4, 0xffffff, true);
                if (costs.getOrDefault(aspect, 0) > 0) graphics.drawString(mc.font, new java.text.DecimalFormat("#######.##").format(
                        costs.get(aspect) * preview.modifiers().getOrDefault(aspect,1f) / 100f), 8, -4, 0xffffff, true);
                pose.popPose();
            }
            pose.popPose();
        }
        if (!focus.isEmpty()) {
            graphics.renderItem(focus, -8, -8);
            float remaining = WandCooldowns.secondsRemaining(player);
            if (remaining > 0) {
                pose.pushPose(); pose.translate(0, 0, 200); pose.scale(.5f, .5f, 1);
                String text = new java.text.DecimalFormat("0.0").format(remaining) + "s";
                graphics.drawString(mc.font, text, -mc.font.width(text) / 2, -4, 0xffffff, true); pose.popPose();
            }
        }
        graphics.setColor(1, 1, 1, 1); pose.popPose();
    }
}
