package conciliummodern;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;

/** One membrane and one uncoated item; copy every existing component/NBT before coating. */
public final class NetherMembraneRecipe extends CustomRecipe {
    public static final String TAG="Nether_Membrane_Tag";
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ThaumicConcilium.ID);
    public static final RegistryObject<SimpleCraftingRecipeSerializer<NetherMembraneRecipe>> SERIALIZER=SERIALIZERS.register("nether_membrane",()->new SimpleCraftingRecipeSerializer<>(NetherMembraneRecipe::new));
    public static final RegistryObject<SimpleCraftingRecipeSerializer<WandXylographyRecipe>> XYLOGRAPHY=SERIALIZERS.register("wand_xylography",()->new SimpleCraftingRecipeSerializer<>(WandXylographyRecipe::new));
    public NetherMembraneRecipe(ResourceLocation id,CraftingBookCategory category) { super(id,category); }
    public static boolean coated(ItemStack stack) { return stack.hasTag() && stack.getTag().getBoolean(TAG); }
    private static ItemStack target(CraftingContainer grid) {
        int membranes=0;ItemStack result=ItemStack.EMPTY;
        for(int slot=0;slot<grid.getContainerSize();slot++) {
            var stack=grid.getItem(slot);if(stack.isEmpty())continue;
            if(stack.is(ConciliumItems.NETHER_MEMBRANE.get())) {if(++membranes>1)return ItemStack.EMPTY;}
            else {
                if(!result.isEmpty() || coated(stack) || stack.hasCraftingRemainingItem())return ItemStack.EMPTY;
                result=stack;
            }
        }
        return membranes==1?result:ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingContainer grid,Level level) { return !target(grid).isEmpty(); }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registry) {
        var original=target(grid);if(original.isEmpty())return ItemStack.EMPTY;
        var result=original.copyWithCount(1);result.getOrCreateTag().putBoolean(TAG,true);return result;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=2; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
}
