package wgmodern.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.item.CosmeticAttachable;
import wgmodern.CloakItem;
import wgmodern.WGAccessories;

/** Botania accepts our interface in removal recipes but only returns its own BaubleItem cosmetics. */
@Pseudo
@Mixin(targets="vazkii.botania.common.crafting.recipe.CosmeticRemoveRecipe",remap=false)
public class BotaniaCosmeticReturnMixin {
    @Inject(method="getRemainingItems(Lnet/minecraft/world/inventory/CraftingContainer;)Lnet/minecraft/core/NonNullList;",at=@At("RETURN"),remap=false)
    private void wg$returnCosmetic(CraftingContainer grid,CallbackInfoReturnable<NonNullList<ItemStack>> cir){
        var remaining=cir.getReturnValue();
        for(int i=0;i<grid.getContainerSize();i++){
            var stack=grid.getItem(i);
            if((stack.getItem() instanceof CloakItem||stack.getItem() instanceof WGAccessories.Gear)
                    &&stack.getItem() instanceof CosmeticAttachable attachable&&remaining.get(i).isEmpty()){
                var cosmetic=attachable.getCosmeticItem(stack);
                if(!cosmetic.isEmpty())remaining.set(i,cosmetic.copyWithCount(1));
            }
        }
    }
}
