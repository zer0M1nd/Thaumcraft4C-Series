package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Fixed vanilla bitmap regions, independent of the selected text font. */
public enum GuiStatusIcon {
    CHECK("minecraft:textures/gui/checkmark.png", 0, 0, 9, 8, 9, 8, false),
    CROSS("realms:textures/gui/realms/cross_player_icon.png", 0, 0, 8, 7, 8, 14, false),
    QUESTION("minecraft:textures/font/ascii.png", 120, 24, 5, 7, 128, 128, true);

    private final ResourceLocation texture;
    private final int u, v, glyphWidth, glyphHeight, textureWidth, textureHeight;
    private final boolean question;

    GuiStatusIcon(String texture, int u, int v, int width, int height, int textureWidth, int textureHeight, boolean question) {
        this.texture = ResourceLocation.parse(texture);
        this.u = u; this.v = v; this.glyphWidth = width; this.glyphHeight = height;
        this.textureWidth = textureWidth; this.textureHeight = textureHeight; this.question = question;
    }
    public int width() { return glyphWidth + (question ? 1 : 0); }
    public int height() { return glyphHeight + (question ? 1 : 0); }
    public void draw(GuiGraphics graphics, int x, int y) {
        graphics.flush();
        if (question) {
            graphics.setColor(.25f, .25f, .08f, 1);
            blit(graphics, x + 1, y + 1);
            graphics.setColor(1, 1, 1f / 3f, 1);
        } else graphics.setColor(1, 1, 1, 1);
        blit(graphics, x, y);
        graphics.setColor(1, 1, 1, 1);
    }
    private void blit(GuiGraphics graphics, int x, int y) {
        LegacyGui.blit(graphics, texture, x, y, u, v, glyphWidth, glyphHeight, textureWidth, textureHeight);
    }
}
