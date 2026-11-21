package thaumicfutures.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.golems.GolemCropRegistry;

/** HarvestCraft 2 crops retain vanilla growth states but need an explicit TC4 crop rule. */
public final class PamsCompat {
    private PamsCompat() {}
    public static void setup() {
        GolemCropRegistry.register(new ResourceLocation("thaumicfutures", "pams_crops"), 10,
                new GolemCropRegistry.Rule() {
            @Override public boolean matches(BlockState state) {
                if (!thaumicfutures.FutureFeatures.Group.FOOD.enabled()) return false;
                var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                return id != null && id.getNamespace().equals("pamhc2crops") && state.getBlock() instanceof CropBlock;
            }
            @Override public boolean mature(ServerLevel level, BlockPos pos, BlockState state) {
                return ((CropBlock) state.getBlock()).isMaxAge(state);
            }
            @Override public boolean harvest(GolemCropRegistry.Context context) {
                if (!context.loaded(context.pos()) || !context.level().getBlockState(context.pos()).equals(context.original())
                        || !mature(context.level(), context.pos(), context.original())) return false;
                CropBlock crop = (CropBlock) context.original().getBlock();
                ItemStack seed = crop.getCloneItemStack(context.level(), context.pos(), context.original());
                if (!context.breakBlock(context.pos(), true)) return false;
                if (context.replant() && !seed.isEmpty()) {
                    for (ItemEntity drop : context.level().getEntitiesOfClass(ItemEntity.class, new AABB(context.pos()).inflate(2))) {
                        if (!drop.isAlive()) continue;
                        int seeds = seedYield(context.level(), drop.getItem(), seed);
                        if (seeds == 0) continue;
                        if (context.place(context.pos(), crop.getStateForAge(0))) {
                            ItemStack remainder = drop.getItem().copy();
                            remainder.shrink(1);
                            if (remainder.isEmpty()) drop.discard(); else drop.setItem(remainder);
                            if (seeds > 1) {
                                var surplus = seed.copy(); surplus.setCount(seeds - 1);
                                net.minecraft.world.level.block.Block.popResource(context.level(), context.pos(), surplus);
                            }
                        }
                        break;
                    }
                }
                return true;
            }
        });
    }
    /** HC2 drops produce rather than seeds. Respect its current one-ingredient seed recipes. */
    private static int seedYield(ServerLevel level, ItemStack ingredient, ItemStack seed) {
        if (ItemStack.isSameItemSameTags(ingredient, seed)) return 1;
        if (ingredient.hasCraftingRemainingItem()) return 0;
        for (var recipe : level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (!(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe)
                    && !(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe) || recipe.isSpecial()) continue;
            var result = recipe.getResultItem(level.registryAccess());
            if (!ItemStack.isSameItemSameTags(result, seed)) continue;
            var inputs = recipe.getIngredients().stream().filter(value -> !value.isEmpty()).toList();
            if (inputs.size() == 1 && inputs.get(0).test(ingredient)) return result.getCount();
        }
        return 0;
    }
}
