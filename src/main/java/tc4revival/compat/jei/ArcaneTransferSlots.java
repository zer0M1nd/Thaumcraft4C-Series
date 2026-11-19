package tc4revival.compat.jei;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.world.item.ItemStack;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
/** Preserve all nine grid positions, including holes; vis is searchable but not an inventory slot. */
public class ArcaneTransferSlots {
    public static List<IRecipeSlotView> items(IRecipeSlotsView slots){
        return slots.getSlotViews().stream().filter(s->s.getRole()==RecipeIngredientRole.INPUT&&s.getSlotName().orElse("").startsWith("items/")).toList();
    }
    public static List<IRecipeSlotView> items(IRecipeSlotsView slots,List<List<ItemStack>> grid){
        var named=items(slots);
        if(named.size()==grid.size())return named;
        // EMI's JEI bridge flattens unnamed inputs, including vis, and drops grid holes.
        // Reconstruct the authoritative item grid instead of interpreting that flat list.
        return java.util.stream.IntStream.range(0,grid.size()).mapToObj(i->(IRecipeSlotView)new GridSlot(i,grid.get(i))).toList();
    }
    private record TransferItem(ItemStack getIngredient) implements ITypedIngredient<ItemStack> {
        public IIngredientType<ItemStack> getType(){return VanillaTypes.ITEM_STACK;}
        public ITypedIngredient<ItemStack> normalize(IIngredientHelper<ItemStack> helper){return new TransferItem(helper.normalizeIngredient(getIngredient));}
    }
    private static final class GridSlot implements IRecipeSlotView {
        private final int index;
        private final List<ITypedIngredient<?>> ingredients;
        GridSlot(int index,List<ItemStack> stacks){this.index=index;ingredients=stacks.stream().filter(s->!s.isEmpty()).<ITypedIngredient<?>>map(s->new TransferItem(s.copyWithCount(1))).toList();}
        public Stream<ITypedIngredient<?>> getAllIngredients(){return ingredients.stream();}
        public List<ITypedIngredient<?>> getAllIngredientsList(){return ingredients;}
        public Optional<ITypedIngredient<?>> getDisplayedIngredient(){return ingredients.stream().findFirst();}
        public Stream<ITypedIngredient<?>> getDisplayedIngredients(){return getDisplayedIngredient().stream();}
        public Optional<net.minecraft.tags.TagKey<?>> getTagKey(){return Optional.empty();}
        public RecipeIngredientRole getRole(){return RecipeIngredientRole.INPUT;}
        public Optional<String> getSlotName(){return Optional.of("items/"+index);}
        public void drawHighlight(net.minecraft.client.gui.GuiGraphics graphics,int color){}
    }
}
