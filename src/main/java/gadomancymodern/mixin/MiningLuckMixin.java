package gadomancymodern.mixin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Forge's common stack lookup covers both loot tables and direct addon enchantment queries. */
@Mixin(ItemStack.class)
public abstract class MiningLuckMixin implements net.minecraftforge.common.extensions.IForgeItemStack {
    @Override public int getEnchantmentLevel(Enchantment enchantment){return ((ItemStack)(Object)this).getItem().getEnchantmentLevel((ItemStack)(Object)this,enchantment)+gadomancymodern.aura.MiningLuck.bonus((ItemStack)(Object)this,enchantment);}
}
