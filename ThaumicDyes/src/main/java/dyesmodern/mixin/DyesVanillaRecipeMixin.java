package dyesmodern.mixin;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=net.minecraft.world.item.crafting.ArmorDyeRecipe.class,remap=false)
public abstract class DyesVanillaRecipeMixin {
    @Inject(method={"matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z","m_5818_(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z"},at=@At("HEAD"),cancellable=true)
    private void dyes$legacyPalette(CraftingContainer grid,Level level,CallbackInfoReturnable<Boolean> cir){for(int i=0;i<grid.getContainerSize();i++)if(grid.getItem(i).getItem() instanceof dyesmodern.DyedArmorItem){cir.setReturnValue(false);return;}}
}
