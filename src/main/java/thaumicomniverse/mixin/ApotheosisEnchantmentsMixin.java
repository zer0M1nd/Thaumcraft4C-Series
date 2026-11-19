package thaumicomniverse.mixin;

import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper", remap=false)
public abstract class ApotheosisEnchantmentsMixin {
    @Redirect(method="selectEnchantment",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getEnchantmentValue()I"),remap=false)
    private static int omniverse$enchantability(ItemStack stack) {
        int value=stack.getEnchantmentValue();
        return value>0?value:thaumicomniverse.compat.ApotheosisEnchantments.repairOverride(stack)||thaumicomniverse.compat.ApotheosisEnchantments.projectEligible(stack)?1:0;
    }
    @Inject(method="getAvailableEnchantmentResults", at=@At("RETURN"), cancellable=true, remap=false)
    private static void omniverse$projectEnchantments(int power, ItemStack stack, boolean treasure, Set<Enchantment> blacklist, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        cir.setReturnValue(thaumicomniverse.compat.ApotheosisEnchantments.additional(cir.getReturnValue(), power, stack, treasure, blacklist));
    }
}
