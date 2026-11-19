package tc4revival.client;

import com.mojang.math.Axis;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.WandFocusItem;
import tc4revival.network.FocusChangePacket;
import tc4revival.network.FocusListRequest;
import tc4revival.network.FocusListReply;
import tc4revival.network.TCNetwork;

/** Non-pausing radial selector; click a focus or release the focus key over it. */
public  class FocusRadialScreen extends Screen {
    private static int nextRequest;
    private final int request = ++nextRequest;
    private boolean requested;
    private final InteractionHand hand;
    private ItemStack wand;
    private final int selectedSlot;
    private final Map<String, ItemStack> foci = new LinkedHashMap<>();
    private final Map<String, Float> sizes = new LinkedHashMap<>();
    private String hovered;
    private float scale;
    private long nextFrame;
    private boolean closing;
    public FocusRadialScreen(InteractionHand hand, ItemStack wand) {
        super(Component.translatable("key.thaumcraft.change_focus")); this.hand = hand; this.wand = wand; this.selectedSlot=Minecraft.getInstance().player.getInventory().selected;
    }
    @Override protected void init() {
        if (!requested) { requested = true; TCNetwork.CHANNEL.sendToServer(new FocusListRequest(request, hand)); }
    }
    public static void receive(FocusListReply packet, net.minecraft.network.Connection connection) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.getConnection().getConnection() != connection
                || !(mc.screen instanceof FocusRadialScreen screen) || screen.request != packet.request() || screen.closing) return;
        // An attached focus is not part of the inventory list. Keep the selector
        // open until key release even when there is no alternative to switch to.
        // Closing on an empty reply reopens it on keyboard auto-repeat.
        screen.foci.clear();
        screen.sizes.clear();
        packet.foci().forEach((key, stack) -> { screen.foci.put(key, stack.copy()); screen.sizes.put(key, 1f); });
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive()
                || hand==InteractionHand.MAIN_HAND && minecraft.player.getInventory().selected!=selectedSlot
                || minecraft.player.getItemInHand(hand).getItem()!=wand.getItem()) { minecraft.setScreen(null); return; }
        // Server inventory synchronization replaces stacks while vis regenerates.
        wand=minecraft.player.getItemInHand(hand);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (minecraft.player == null) return;
        long now = System.currentTimeMillis();
        if (now > nextFrame) {
            scale = Math.max(0, Math.min(1, scale + (closing ? -.05f : .05f)));
            for (var key : sizes.keySet()) sizes.put(key, Math.max(1, Math.min(1.3f, sizes.get(key) + (key.equals(hovered) ? .025f : -.025f))));
            nextFrame = now + 5;
        }
        if (closing && scale == 0) { minecraft.setScreen(null); return; }
        float radius = 16 + foci.size() * 2.5f;
        var pose = graphics.pose(); pose.pushPose(); pose.translate(width / 2f, height / 2f, 200);
        float rotation = partial + minecraft.player.tickCount % 720 / 2f;
        ring(graphics, "radial", radius * 2.75f * scale, rotation);
        ring(graphics, "radial2", radius * 2.55f * scale, -rotation);
        ItemStack tooltip = ItemStack.EMPTY;
        if (!closing) hovered = null;
        var current = WandFocusItem.attached(wand);
        if (!current.isEmpty()) {
            var icon = current.copy(); icon.setTag(null); graphics.renderItem(icon, -8, -8);
            if (Math.abs(mouseX - width / 2f) <= 10 && Math.abs(mouseY - height / 2f) <= 10) tooltip = current;
        }
        float angle = -90 * scale, slice = foci.isEmpty() ? 0 : 360f / foci.size();
        for (var entry : foci.entrySet()) {
            float x = (float) Math.cos(Math.toRadians(angle)) * radius, y = (float) Math.sin(Math.toRadians(angle)) * radius; angle += slice;
            pose.pushPose(); pose.translate(x, y, 0); float size = sizes.get(entry.getKey()); pose.scale(size, size, 1);
            var icon = entry.getValue().copy(); icon.setTag(null); graphics.renderItem(icon, -8, -8); pose.popPose();
            if (!closing && Math.abs(mouseX - width / 2f - x) <= 10 && Math.abs(mouseY - height / 2f - y) <= 10) {
                hovered = entry.getKey(); tooltip = entry.getValue();
            }
        }
        pose.popPose();
        if (!tooltip.isEmpty()) graphics.renderTooltip(font, tooltip, width / 2 - 4, height / 2 + 20);
    }
    private void ring(GuiGraphics graphics, String texture, float size, float rotation) {
        var pose = graphics.pose(); pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(rotation)); pose.scale(size, size, 1);
        graphics.setColor(.5f, .5f, .5f, .5f);
        pose.translate(-.5, -.5, 0);
        LegacyGui.blit(graphics,ResourceLocation.parse("thaumcraft:textures/misc/" + texture + ".png"), 0, 0, 0, 0, 1, 1, 1, 1);
        graphics.setColor(1, 1, 1, 1); pose.popPose();
    }
    private void select() {
        if (closing) return;
        if (hovered != null) TCNetwork.CHANNEL.sendToServer(new FocusChangePacket(hand, hovered));
        closing = true;
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (WandKeys.CHANGE_FOCUS.matches(key, scan)) { select(); return true; }
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && hovered != null) { select(); return true; }
        return super.mouseClicked(x, y, button);
    }
}
