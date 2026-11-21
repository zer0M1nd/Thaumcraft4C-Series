package thmodern.client;
import tc4revival.client.LegacyGui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.aspect.TCAspects;
import thmodern.vat.MorphicFingersMenu;
import tc4revival.content.CastingWandItem;

public final class MorphicFingersScreen extends AbstractContainerScreen<MorphicFingersMenu> {
    private static final int[][] POSITIONS = {{72,21},{24,43},{24,102},{72,124},{120,102},{120,43}};
    public MorphicFingersScreen(MorphicFingersMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 190; imageHeight = 234;
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
        int index = 0;
        for (var aspect : thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.LEGACY_SIX)) {
            int x = leftPos + POSITIONS[index][0] - 8, y = topPos + POSITIONS[index++][1] - 8;
            if (menu.visCost().getOrDefault(aspect, 0) > 0 && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                graphics.renderTooltip(font, Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()), mouseX, mouseY);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        menu.refresh();
        LegacyGui.blit(graphics,ResourceLocation.parse("thaumichorizons:textures/gui/guifingers.png"), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        var stack = menu.wand(); var wand = stack.getItem() instanceof CastingWandItem value ? value : null;
        int index = 0; boolean enough = true;
        for (var aspect : thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.LEGACY_SIX)) {
            float cost = menu.visCost().getOrDefault(aspect, 0);
            int x = leftPos + POSITIONS[index][0] - 8, y = topPos + POSITIONS[index][1] - 8;
            if (cost > 0) {
                if (wand != null) cost *= wand.consumptionModifier(stack, minecraft.player, aspect, true);
                boolean paid = wand != null && cost * 100 <= wand.vis(stack, aspect); enough &= paid;
                float alpha = paid ? 1 : .3f + (float) Math.sin((minecraft.player.tickCount + index * 10) / 2f) * .2f;
                int color = thaumcraft.api.aspects.AspectViews.display(aspect).color();
                graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha);
                LegacyGui.blit(graphics,thaumcraft.api.aspects.AspectViews.display(aspect).texture(), x, y, 0, 0, 16, 16, 16, 16);
                graphics.setColor(1, 1, 1, 1);
                String text = new java.text.DecimalFormat("#######.##").format(cost);
                graphics.drawString(font, text, x + 16 - font.width(text), y + 8, ((int) (alpha * 255) << 24) | 0xffffff, true);
            }
            index++;
        }
        if (wand != null && !enough && menu.getSlot(0).getItem().isEmpty() && !menu.preview().isEmpty()) {
            graphics.setColor(.33f, .33f, .33f, .66f); graphics.renderItem(menu.preview(), leftPos + 160, topPos + 64);
            graphics.renderItemDecorations(font, menu.preview(), leftPos + 160, topPos + 64); graphics.setColor(1, 1, 1, 1);
            graphics.pose().pushPose(); graphics.pose().translate(leftPos + 168, topPos + 46, 0); graphics.pose().scale(.5f, .5f, 1);
            var message = Component.translatable("thaumcraft.gui.insufficient_vis"); graphics.drawString(font, message, -font.width(message) / 2, 0, 0xee6e6e, false); graphics.pose().popPose();
        }
    }
}
