package tc4revival.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import tc4revival.content.NuggetType;
import tc4revival.content.TCItems;

/** Permanent checks against recipes actually loaded by the dedicated server. */
 class NuggetDebugChecks {
    private NuggetDebugChecks() {}
    static int run(CommandSourceStack source) {
        var menu = new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var level = source.getServer().overworld();
        var recipes = level.getRecipeManager();
        int placements = 0, rejections = 0;
        for (var type : NuggetType.values()) {
            var nugget = new ItemStack(TCItems.nugget(type));
            var identity = tc4revival.aspect.item.ItemStackAspects.identity(nugget).orElseThrow();
            require(identity.item().equals("Thaumcraft:ItemNugget") && identity.metadata() == type.metadata(), "Nugget identity");
            require(nugget.getMaxStackSize() == 64 && !nugget.isDamageableItem(), "Nugget properties");
            type.tag().ifPresent(tag -> require(nugget.is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(tag))), "Nugget tag"));
            for (int width : new int[]{2, 3}) {
                var grid = new TransientCraftingContainer(menu, width, width);
                for (int slot = 0; slot < grid.getContainerSize(); slot++) {
                    grid.clearContent();
                    grid.setItem(slot, new ItemStack(type.materialItem(), 64));
                    var recipe = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
                    if (type == NuggetType.COPPER) require(recipe.getIngredients().get(0).toJson().getAsJsonObject()
                            .get("tag").getAsString().equals("forge:ingots/copper"), "Copper ore dictionary input was not migrated to a tag");
                    var result = recipe.assemble(grid, level.registryAccess());
                    require(result.is(TCItems.nugget(type)) && result.getCount() == 9, "Split output or count");
                    require(grid.getItem(slot).getCount() == 64, "Recipe assembly modified input");
                    require(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty), "Unexpected crafting remainder");
                    grid.setItem((slot + 1) % grid.getContainerSize(), new ItemStack(Items.DIRT));
                    require(recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Extra ingredient accepted");
                    placements++; rejections++;
                }
                grid.clearContent();
                for (int slot = 0; slot < grid.getContainerSize(); slot++) grid.setItem(slot, nugget.copy());
                if (width == 2) {
                    require(recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Four nuggets compressed");
                    rejections++; continue;
                }
                var recipe = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
                var result = recipe.assemble(grid, level.registryAccess());
                require(result.is(type.materialItem()) && result.getCount() == 1, "Compression output");
                for (int slot = 0; slot < 9; slot++) {
                    grid.setItem(slot, ItemStack.EMPTY);
                    require(recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Eight nuggets compressed");
                    grid.setItem(slot, new ItemStack(Items.GOLD_NUGGET));
                    require(recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Foreign nugget accepted");
                    grid.setItem(slot, nugget.copy()); rejections += 2;
                }
            }
        }
        // The misleading old ore name refers to the whole material, never to its ninth.
        require(!new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER)).is(net.minecraft.tags.ItemTags.create(
                ResourceLocation.parse("forge:nuggets/quicksilver"))), "Quicksilver drop changed old ore membership");
        require(NuggetType.COPPER.materialItem() == Items.COPPER_INGOT
                && tc4revival.content.ClusterType.COPPER.output() == Items.COPPER_INGOT, "Copper did not reuse vanilla ingot");
        var aspects = tc4revival.aspect.TCAspects.catalog();
        var dropAspects = tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(TCItems.nugget(NuggetType.COPPER))).orElseThrow();
        require(dropAspects.size() == 1 && dropAspects.amount(aspects.require("metallum")) == 1, "Copper nugget conditional aspects missing");
        var clusterAspects = tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(TCItems.cluster(tc4revival.content.ClusterType.COPPER))).orElseThrow();
        require(clusterAspects.size() == 4 && clusterAspects.amount(aspects.require("metallum")) == 5
                && clusterAspects.amount(aspects.require("ordo")) == 1 && clusterAspects.amount(aspects.require("terra")) == 1
                && clusterAspects.amount(aspects.require("permutatio")) == 2, "Copper cluster conditional aspects missing");
        int checkedPlacements = placements, checkedRejections = rejections;
        source.sendSuccess(() -> Component.literal("nuggets PASS: " + NuggetType.values().length + " families, " + NuggetType.values().length * 2 + " loaded recipes, " + checkedPlacements
                + " split placements, " + checkedRejections + " invalid grids rejected; no player inventory changes"), false);
        return 1;
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
