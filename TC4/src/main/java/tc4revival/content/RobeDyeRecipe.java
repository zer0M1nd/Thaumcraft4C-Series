package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Retains ordinary vanilla dyeing, but uses the old sheep palette and mandatory base colour for TC4 robes. */
public  class RobeDyeRecipe extends ArmorDyeRecipe {
    // Numeric palette verified from Minecraft 1.7.10 EntitySheep (wp.bp) static initializer.
    private static final float[][] COLORS = {{1,1,1},{.85f,.5f,.2f},{.7f,.3f,.85f},{.4f,.6f,.85f},
            {.9f,.9f,.2f},{.5f,.8f,.1f},{.95f,.5f,.65f},{.3f,.3f,.3f},{.6f,.6f,.6f},{.3f,.5f,.6f},
            {.5f,.25f,.7f},{.2f,.3f,.7f},{.4f,.3f,.2f},{.4f,.5f,.2f},{.6f,.2f,.2f},{.1f,.1f,.1f}};
    public RobeDyeRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    private static boolean isRobe(ItemStack stack) { return stack.getItem() instanceof RobeArmorItem || stack.getItem() instanceof VoidRobeArmorItem; }
    private static boolean containsRobe(CraftingContainer grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) if (isRobe(grid.getItem(i))) return true;
        return false;
    }
    private static DyeColor dye(ItemStack stack) {
        if (stack.getItem() instanceof DyeItem dye) return dye.getDyeColor();
        if (stack.is(Items.BONE_MEAL)) return DyeColor.WHITE;
        if (stack.is(Items.INK_SAC)) return DyeColor.BLACK;
        if (stack.is(Items.LAPIS_LAZULI)) return DyeColor.BLUE;
        if (stack.is(Items.COCOA_BEANS)) return DyeColor.BROWN;
        return null;
    }
    @Override public boolean matches(CraftingContainer grid, Level level) {
        var extension=thaumcraft.api.crafting.RobeDyeing.planDye(thaumcraft.api.crafting.RobeDyeing.CraftingGridSnapshot.of(grid));
        if(extension.isPresent())return extension.get().matches();
        if (!containsRobe(grid)) return super.matches(grid, level);
        int robes = 0, dyes = 0;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            var stack = grid.getItem(i); if (stack.isEmpty()) continue;
            if (isRobe(stack)) robes++;
            else if (dye(stack) != null) dyes++; else return false;
        }
        return robes == 1 && dyes > 0;
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        var extension=thaumcraft.api.crafting.RobeDyeing.planDye(thaumcraft.api.crafting.RobeDyeing.CraftingGridSnapshot.of(grid));
        if(extension.isPresent())return extension.get().output();
        if (!containsRobe(grid)) return super.assemble(grid, registries);
        ItemStack robe = ItemStack.EMPTY; List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            var stack = grid.getItem(i); if (stack.isEmpty()) continue;
            if (isRobe(stack)) {
                if (!robe.isEmpty()) return ItemStack.EMPTY;
                robe = stack.copyWithCount(1); colors.add(((net.minecraft.world.item.DyeableLeatherItem)stack.getItem()).getColor(stack));
            } else {
                var dye = dye(stack); if (dye == null) return ItemStack.EMPTY;
                float[] color = COLORS[dye.getId()]; colors.add((int) (color[0] * 255) << 16 | (int) (color[1] * 255) << 8 | (int) (color[2] * 255));
            }
        }
        if (robe.isEmpty() || colors.size() < 2) return ItemStack.EMPTY;
        ((net.minecraft.world.item.DyeableLeatherItem) robe.getItem()).setColor(robe, blend(colors)); return robe;
    }
    public static int blend(List<Integer> colors) {
        if (colors.isEmpty()) throw new IllegalArgumentException("No dye colours");
        long red = 0, green = 0, blue = 0, brightness = 0;
        for (int color : colors) {
            int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
            red += r; green += g; blue += b; brightness += Math.max(r, Math.max(g, b));
        }
        int r = (int) (red / colors.size()), g = (int) (green / colors.size()), b = (int) (blue / colors.size());
        int peak = Math.max(r, Math.max(g, b)); if (peak == 0) return 0;
        float mean = (float) brightness / colors.size();
        return (int) (r * mean / peak) << 16 | (int) (g * mean / peak) << 8 | (int) (b * mean / peak);
    }
    @Override public RecipeSerializer<?> getSerializer() { return TCArcane.ROBE_DYE_SERIALIZER.get(); }
}
