package tc4revival.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/** Reuses the old potion atlas without resampling its 18-pixel icons. */
public record LegacyEffectIcon(java.util.function.IntSupplier column, java.util.function.IntSupplier row) implements IClientMobEffectExtensions {
    public LegacyEffectIcon(int column,int row) { this(()->column,()->row); }
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/misc/potions.png");
    @Override public boolean renderInventoryIcon(MobEffectInstance effect, EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics, int x, int y, int z) {
        LegacyGui.blit(graphics,TEXTURE, x, y + 7, column.getAsInt() * 18, 198 + row.getAsInt() * 18, 18, 18); return true;
    }
    @Override public boolean renderGuiIcon(MobEffectInstance effect, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
        graphics.setColor(1, 1, 1, alpha); LegacyGui.blit(graphics,TEXTURE, x + 3, y + 3, column.getAsInt() * 18, 198 + row.getAsInt() * 18, 18, 18);
        graphics.setColor(1, 1, 1, 1); return true;
    }
}
