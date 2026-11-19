package thaumicfutures.mixin;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicfutures.compat.exdeorum.ExDeorumCompat;

/** Nitor has particle-only world rendering; JEI must use its existing item icon. */
@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.compat.jei.CrucibleHeatSourcesCategory", remap = false)
public abstract class ExDeorumHeatDisplayMixin {
    @Inject(method = "draw(Lthedarkcolour/exdeorum/compat/jei/CrucibleHeatSourceRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",
            at = @At("HEAD"), cancellable = true)
    private void futures$nitorIcon(@Coerce Object recipe, IRecipeSlotsView slots,
            GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo callback) {
        var heat = (ExDeorumHeatRecipeAccessor) recipe;
        var block = heat.futures$blockState().getBlock();
        if (!ResourceLocation.parse("thaumcraft:nitor").equals(ForgeRegistries.BLOCKS.getKey(block))) return;
        var level = Minecraft.getInstance().level;
        var nativeRecipe = level == null ? null : level.getRecipeManager()
                .byKey(ResourceLocation.parse("thaumicfutures:exdeorum/heat_nitor")).orElse(null);
        boolean legacyRate = ExDeorumCompat.enabled() && heat.futures$meltRate() == 2
                && nativeRecipe instanceof thedarkcolour.exdeorum.recipe.crucible.CrucibleHeatRecipe nitor
                && nitor.heatValue() == 2 && nitor.blockPredicate().test(heat.futures$blockState());
        Object multiplier = legacyRate ? "1.5" : heat.futures$meltRate();
        var label = Component.translatable("gui.exdeorum.category.crucible_heat_source.multiplier", multiplier);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, label, 60 - font.width(label) / 2, 5, 0xff808080, false);
        graphics.renderItem(new ItemStack(block), 52, 24);
        callback.cancel();
    }
}
