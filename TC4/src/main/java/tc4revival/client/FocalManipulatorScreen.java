package tc4revival.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.FocalManipulatorBlockEntity;
import tc4revival.content.FocalManipulatorMenu;
import tc4revival.content.FocusUpgrade;
import tc4revival.content.TCSounds;
import tc4revival.content.WandFocusItem;

public  class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("thaumcraft:textures/gui/gui_wandtable.png");
    private static final ResourceLocation PARTICLES = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private ItemStack previousFocus = ItemStack.EMPTY;
    private int selected = -1, rank, installedCount;
    private final List<Integer> queued = new ArrayList<>();
    private tc4revival.content.FocalUpgradePlan plan;
    private int experienceCost() { return menu.batchEnabled() && plan != null ? plan.experience() : tc4revival.content.FocalUpgradePlan.experienceForCount(1); }
    private boolean affordable() { return minecraft.player.getAbilities().instabuild
            || tc4revival.content.FocalUpgradePlan.availablePoints(minecraft.player)
                >= tc4revival.content.FocalUpgradePlan.experiencePoints(experienceCost()); }
    private List<FocusUpgrade> choices = List.of(), applied = List.of();
    private final List<Sparkle> sparkles = new ArrayList<>();
    private long nextSparkle;
    private int visScroll;
    private AspectAmounts displayedCost = new AspectAmounts();
    private static  class Sparkle {
        float x, y;
        final float dx, dy, red, green, blue;
        long nextFrame;
        int frame;
        Sparkle(float x, float y, float dx, float dy, float red, float green, float blue) {
            this.x = x; this.y = y; this.dx = dx; this.dy = dy; this.red = red; this.green = green; this.blue = blue;
        }
    }
    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 192; imageHeight = 233;
    }
    private void gather() {
        var stack = menu.focus();
        if (!ItemStack.matches(stack, previousFocus)) { selected = -1; queued.clear(); previousFocus = stack.copy(); }
        if (!menu.batchEnabled()) queued.clear();
        var done = new ArrayList<FocusUpgrade>(); plan = null;
        if (stack.getItem() instanceof WandFocusItem focus) {
            installedCount = focus.nextRank(stack) - 1;
            var preview = stack;
            if (menu.size() > 0) { queued.clear(); queued.addAll(menu.queue()); }
            if (!queued.isEmpty()) {
                plan = tc4revival.content.FocalUpgradePlan.create(stack, queued, minecraft.player);
                if (plan == null) queued.clear(); else preview = plan.result();
            }
            rank = focus.nextRank(preview);
            for (short id : focus.appliedUpgrades(preview)) { if (id == -1) break; FocusUpgrade.byId(id).ifPresent(done::add); }
            final var candidate = preview;
            choices = rank > 5 ? List.of() : focus.availableUpgrades(candidate, rank).stream()
                    .filter(upgrade -> focus.canApplyUpgrade(candidate, minecraft.player, upgrade, rank)
                            && thaumcraft.api.wands.FocusUpgradeType.canApply(upgrade, candidate, minecraft.player, rank)).toList();
            if (!queued.isEmpty()) { selected = queued.get(queued.size() - 1); rank = installedCount + 1; }
            else if (rank > 5) rank = -1;
        } else { rank = 0; installedCount = 0; choices = List.of(); selected = -1; queued.clear(); }
        applied = List.copyOf(done);
        if (menu.size() > 0) { rank = menu.rank(); selected = menu.upgrade(); choices = List.of(); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < choices.size(); i++) if (inside(mouseX, mouseY, 48 + i * 16, 104, 16, 16)) tooltip(graphics, choices.get(i), mouseX, mouseY);
        for (int i = 0; i < applied.size(); i++) if (inside(mouseX, mouseY, 56 + i * 16, 32, 16, 16)) tooltip(graphics, applied.get(i), mouseX, mouseY);
        if (selected >= 0) {
            if (inside(mouseX, mouseY, 108, 58, 36, 16)) graphics.renderTooltip(font, Component.translatable("wandtable.text2"), mouseX, mouseY);
            if (menu.size() == 0 && affordable() && inside(mouseX, mouseY, 48, 88, 96, 8))
                graphics.renderTooltip(font, Component.translatable("wandtable.text3"), mouseX, mouseY);
        }
        if (inside(mouseX, mouseY, 48, 48, 37, 36)) {
            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.add(Component.translatable("thaumcraft.focal.stored_vis").getVisualOrderText());
            var stored = menu.stored();
            for (var aspect : menu.primals()) lines.add(Component.literal(visLine(aspect, stored))
                    .withStyle(style -> style.withColor(thaumcraft.api.client.ClientAspectViews.view(aspect).color())).getVisualOrderText());
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        }
    }
    private String visLine(tc4revival.aspect.Aspect aspect, AspectAmounts stored) {
        var format = new java.text.DecimalFormat("0.#");
        String value = format.format(stored.amount(aspect) / 100f);
        if (!displayedCost.isEmpty()) value = format.format(displayedCost.amount(aspect) / 100f) + "/" + value;
        return thaumcraft.api.client.ClientAspectViews.view(aspect).displayName() + " " + value;
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (inside(x, y, 48, 48, 37, 36)) {
            visScroll = Math.max(0, Math.min(Math.max(0, menu.primals().size() - 6), visScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }
    private void tooltip(GuiGraphics graphics, FocusUpgrade upgrade, int mouseX, int mouseY) {
        var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        lines.add(Component.translatable(upgrade.nameKey()).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.UNDERLINE).getVisualOrderText());
        lines.addAll(font.split(Component.translatable(upgrade.descriptionKey()), Math.max(90, width - leftPos - imageWidth + 16)));
        graphics.renderTooltip(font, lines, leftPos + imageWidth - 36, topPos + 24);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        gather(); LegacyGui.blit(graphics,BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < choices.size(); i++) {
            if (selected == choices.get(i).id) LegacyGui.blit(graphics,BACKGROUND, leftPos + 48 + i * 16, topPos + 104, 200, 0, 16, 16);
            icon(graphics, choices.get(i), 48 + i * 16, 104);
        }
        for (int i = 0; i < applied.size(); i++) {
            if (i >= installedCount) graphics.fill(leftPos + 56 + i * 16, topPos + 32, leftPos + 72 + i * 16, topPos + 48, 0x803799ba);
            icon(graphics, applied.get(i), 56 + i * 16, 32);
        }
        var option = FocusUpgrade.byId(selected);
        AspectAmounts amounts = menu.size() > 0 ? menu.remaining() : plan != null ? plan.cost()
                : rank > 0 && option.isPresent() ? FocalManipulatorBlockEntity.cost(option.get(), rank) : new AspectAmounts();
        displayedCost = amounts;
        if (rank > 0 && option.isPresent()) {
            int xp = experienceCost();
            if (menu.size() == 0 && affordable()) LegacyGui.blit(graphics,BACKGROUND, leftPos + 48, topPos + 88, 8, 240, 96, 8);
            LegacyGui.blit(graphics,BACKGROUND, leftPos + 108, topPos + 59, 200, 16, 16, 16);
            graphics.drawString(font, Integer.toString(xp), leftPos + 125, topPos + 64, !affordable() ? 16151160 : 10092429);
            int offset = 0;
            for (var aspect : amounts.alphabetical()) {
                int color = thaumcraft.api.client.ClientAspectViews.view(aspect).color();
                if (menu.size() > 0) {
                    int length = Math.max(0, Math.min(96 - offset, (int) (amounts.amount(aspect) * 96L / menu.size())));
                    graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, .9f);
                    LegacyGui.blit(graphics,BACKGROUND, leftPos + 48 + offset, topPos + 88, 112 + offset, 240, length, 8);
                    graphics.setColor(1, 1, 1, 1); offset += length;
                    if (minecraft.level.random.nextInt(66) == 0) sparkles.add(new Sparkle(48 + offset, 92, (46 + rank * 16 - 48 - offset) / 9f, -6,
                            (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f));
                }
            }
        }
        var stored = menu.stored();
        int row = 0;
        for (var aspect : menu.primals().stream().skip(visScroll).limit(6).toList()) {
            String line = visLine(aspect, stored);
            float scale = Math.min(.5f, 36f / font.width(line));
            graphics.pose().pushPose(); graphics.pose().translate(leftPos + 49, topPos + 53 + row++ * 5, 0);
            graphics.pose().scale(scale, scale, scale);
            graphics.drawString(font, line, 0, 0, thaumcraft.api.client.ClientAspectViews.view(aspect).color());
            graphics.pose().popPose();
        }
        long now = System.currentTimeMillis();
        if (rank > 0 && now > nextSparkle) {
            var random = minecraft.level.random; nextSparkle = now + (menu.size() > 0 ? 10 : 500) + random.nextInt(200);
            sparkles.add(new Sparkle(42 + rank * 16 + random.nextInt(12), 34 + random.nextInt(12), 0, 0,
                    .5f + random.nextFloat() * .4f, 1 - random.nextFloat() * .4f, 1 - random.nextFloat() * .4f));
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        for (var sparkle : sparkles) {
            graphics.pose().pushPose(); graphics.pose().translate(sparkle.x, sparkle.y, 200);
            graphics.setColor(sparkle.red, sparkle.green, sparkle.blue, .9f);
            LegacyGui.blit(graphics,PARTICLES, -4, -4, 8, 8, sparkle.frame * 16, 112, 16, 16, 256, 256);
            graphics.setColor(1, 1, 1, 1); graphics.pose().popPose();
            if (now > sparkle.nextFrame) { sparkle.frame++; sparkle.nextFrame = now + 50; sparkle.x += sparkle.dx; sparkle.y += sparkle.dy; }
        }
        sparkles.removeIf(sparkle -> sparkle.frame >= 9);
    }
    private void icon(GuiGraphics graphics, FocusUpgrade upgrade, int x, int y) {
        LegacyGui.blit(graphics,upgrade.texture(), leftPos + x, topPos + y, 0, 0, 16, 16, 16, 16);
    }
    private boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= leftPos + left && x < leftPos + left + width && y >= topPos + top && y < topPos + top + height;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        gather();
        if (menu.size() == 0 && selected >= 0 && rank > 0 && affordable() && inside(x, y, 48, 88, 96, 8)) {
            if (menu.batchEnabled()) {
                if (plan == null) return true;
                tc4revival.network.TCNetwork.CHANNEL.sendToServer(new tc4revival.network.FocalBatchPacket(menu.containerId, menu.focus().copy(), List.copyOf(queued)));
            } else minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
            clickSound(); return true;
        }
        if (menu.size() == 0 && menu.batchEnabled()) {
            for (int i = installedCount; i < applied.size(); i++) if (inside(x, y, 56 + i * 16, 32, 16, 16)) {
                queued.subList(i - installedCount, queued.size()).clear(); selected = -1; clickSound(); return true;
            }
        }
        if (menu.size() == 0) for (int i = 0; i < choices.size(); i++) if (inside(x, y, 48 + i * 16, 104, 16, 16)) {
            if (menu.batchEnabled()) queued.add(choices.get(i).id);
            else selected = selected == choices.get(i).id ? -1 : choices.get(i).id;
            clickSound(); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    private void clickSound() { minecraft.player.playSound(TCSounds.CAMERA_CLACK.get(), .4f, 1); }
}
