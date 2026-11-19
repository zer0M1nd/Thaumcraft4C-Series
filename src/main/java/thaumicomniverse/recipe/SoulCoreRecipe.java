package thaumicomniverse.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import tc4revival.content.CastingWandItem;
import thaumcraft.api.wands.WandAutomation;
import thaumicomniverse.content.SoulCoreItem;

/** Install/remove a core, or copy a binding while retaining the original bound core. */
public class SoulCoreRecipe extends CustomRecipe {
    public SoulCoreRecipe(ResourceLocation id, CraftingBookCategory category) { super(id,category); }
    private record Inputs(int slot, ItemStack wand, ItemStack core, boolean remove) {}
    private record Copy(int slot, ItemStack bound, ItemStack blank) {}
    private Copy copy(CraftingContainer grid) {
        if (!thaumicomniverse.RevivalFeatures.Group.SOUL.enabled()) return null;
        int slot = -1;
        ItemStack bound = ItemStack.EMPTY, blank = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            var stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getCount() != 1 || !(stack.getItem() instanceof SoulCoreItem core)) return null;
            if (core.owner(stack) == null && blank.isEmpty()) blank = stack;
            else if (core.owner(stack) != null && bound.isEmpty()) { bound = stack; slot = i; }
            else return null;
        }
        return bound.isEmpty() || blank.isEmpty() ? null : new Copy(slot, bound, blank);
    }
    private Inputs inputs(CraftingContainer grid) {
        int slot=-1;ItemStack wand=ItemStack.EMPTY,core=ItemStack.EMPTY;
        for(int i=0;i<grid.getContainerSize();i++) {
            var stack=grid.getItem(i);if(stack.isEmpty())continue;
            if(stack.getCount()!=1)return null;
            if(stack.getItem() instanceof CastingWandItem && wand.isEmpty()) {wand=stack;slot=i;}
            else if(stack.getItem() instanceof SoulCoreItem item && item.owner(stack)!=null && core.isEmpty())core=stack;
            else return null;
        }
        if(wand.isEmpty())return null;
        boolean remove=WandAutomation.installed(wand);
        if(!remove&&!thaumicomniverse.RevivalFeatures.Group.SOUL.enabled())return null;
        if(remove ? !core.isEmpty() || !(WandAutomation.core(wand).getItem() instanceof SoulCoreItem) : core.isEmpty())return null;
        return new Inputs(slot,wand,core,remove);
    }
    @Override public boolean matches(CraftingContainer grid,Level level) { return copy(grid)!=null || inputs(grid)!=null; }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registries) {
        var copied = copy(grid);
        if (copied != null) {
            var result = copied.blank().copy();
            result.getOrCreateTag().putUUID("Owner", copied.bound().getTag().getUUID("Owner"));
            result.getOrCreateTag().putString("OwnerName", copied.bound().getTag().getString("OwnerName"));
            return result;
        }
        var input=inputs(grid);if(input==null)return ItemStack.EMPTY;
        var result=input.wand().copy();
        if(input.remove())result.removeTagKey(WandAutomation.KEY);
        else result.getOrCreateTag().put(WandAutomation.KEY,input.core().save(new net.minecraft.nbt.CompoundTag()));
        return result;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        var result=NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);var input=inputs(grid);
        var copied = copy(grid);
        if (copied != null) { result.set(copied.slot(), copied.bound().copy()); return result; }
        if(input!=null && input.remove())result.set(input.slot(),WandAutomation.core(input.wand()).copy());
        return result;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public RecipeSerializer<?> getSerializer() { return CollaborationRecipes.SOUL_CORE.get(); }
}
