package temodern.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.inventory.CraftingContainer;
import tc4revival.content.TCItems;
import tc4revival.content.EssentiaPhialItem;

/** Match the main arcane workbench's reusable catalysts, including their NBT. */
public final class CraftRemainders {
    public static NonNullList<ItemStack> forPlayer(Recipe<CraftingContainer> recipe, CraftGrid grid,
            ItemStack output, net.minecraft.world.entity.player.Player player) {
        var previous=net.minecraftforge.common.ForgeHooks.getCraftingPlayer();
        net.minecraftforge.common.ForgeHooks.setCraftingPlayer(player);
        try { return all(recipe,grid,output); }
        finally { net.minecraftforge.common.ForgeHooks.setCraftingPlayer(previous); }
    }
    public static boolean catalyst(ItemStack output, CraftGrid grid, int slot) {
        return output.is(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()) && slot == 4
                || output.is(TCItems.JAR_LABEL.get()) && output.hasTag()
                && grid.getItem(slot).getItem() instanceof EssentiaPhialItem;
    }
    public static NonNullList<ItemStack> all(Recipe<CraftingContainer> recipe, CraftGrid grid, ItemStack output) {
        var remaining = recipe.getRemainingItems(grid);
        for (int i=0;i<9;i++) if (catalyst(output,grid,i)) remaining.set(i,grid.getItem(i).copyWithCount(1));
        return remaining;
    }
}
