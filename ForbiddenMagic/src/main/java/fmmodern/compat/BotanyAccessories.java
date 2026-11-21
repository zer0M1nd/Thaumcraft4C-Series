package fmmodern.compat;

import fmmodern.*;
import net.minecraft.world.item.ItemStack;
import vazkii.botania.api.item.CosmeticAttachable;

/** Botania's attachment/removal recipes and Curios renderer consume this public interface. */
public final class BotanyAccessories {
    public static net.minecraft.world.item.Item ring(){return new Ring();}
    public static net.minecraft.world.item.Item collar(){return new Collar();}
    private static ItemStack read(ItemStack stack){return stack.hasTag()?ItemStack.of(stack.getTag().getCompound("cosmeticItem")):ItemStack.EMPTY;}
    private static void write(ItemStack stack,ItemStack cosmetic){if(cosmetic.isEmpty())stack.getOrCreateTag().remove("cosmeticItem");else stack.getOrCreateTag().put("cosmeticItem",cosmetic.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()));}
    public static final class Ring extends SpecialItems.NutritionRing implements CosmeticAttachable {
        public ItemStack getCosmeticItem(ItemStack stack){return read(stack);}public void setCosmeticItem(ItemStack stack,ItemStack cosmetic){write(stack,cosmetic);}
    }
    public static final class Collar extends PainCollar implements CosmeticAttachable {
        public ItemStack getCosmeticItem(ItemStack stack){return read(stack);}public void setCosmeticItem(ItemStack stack,ItemStack cosmetic){write(stack,cosmetic);}
    }
    private BotanyAccessories(){}
}
