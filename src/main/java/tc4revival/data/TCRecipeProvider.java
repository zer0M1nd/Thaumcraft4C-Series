package tc4revival.data;

import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import tc4revival.content.TCItems;

public  class TCRecipeProvider extends RecipeProvider {
    public TCRecipeProvider(PackOutput output) { super(output); }
    @Override protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        for(int ore:new int[]{0,7}) net.minecraft.data.recipes.SimpleCookingRecipeBuilder.smelting(net.minecraft.world.item.crafting.Ingredient.of(ItemTags.create(ResourceLocation.parse(tc4revival.content.LegacyOreBlock.tag(ore)))),RecipeCategory.MISC,TCItems.material(ore==0?tc4revival.content.MaterialType.QUICKSILVER:tc4revival.content.MaterialType.AMBER),1,200).unlockedBy("has_ore",has(tc4revival.content.TCArcane.ore(ore))).save(writer,"thaumcraft:"+tc4revival.content.LegacyOreBlock.path(ore)+"_smelting");
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE,TCItems.METAL_GRATE.get()).pattern("I").pattern("T").define('I',net.minecraft.world.level.block.Blocks.IRON_BARS).define('T',net.minecraft.world.level.block.Blocks.OAK_TRAPDOOR).unlockedBy("has_iron",has(Items.IRON_INGOT)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,TCItems.THAUMIUM_BLOCK.get()).pattern("III").pattern("III").pattern("III")
                .define('I',ItemTags.create(ResourceLocation.parse("forge:ingots/thaumium"))).unlockedBy("has_thaumium",has(TCItems.material(tc4revival.content.MaterialType.THAUMIUM))).save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,TCItems.material(tc4revival.content.MaterialType.THAUMIUM),9).requires(TCItems.THAUMIUM_BLOCK.get())
                .unlockedBy("has_thaumium_block",has(TCItems.THAUMIUM_BLOCK.get())).save(writer,"thaumcraft:thaumium_from_block");
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,tc4revival.content.TCCleansing.TALLOW_BLOCK.get()).pattern("TTT").pattern("TTT").pattern("TTT")
                .define('T',TCItems.material(tc4revival.content.MaterialType.TALLOW)).unlockedBy("has_tallow",has(TCItems.material(tc4revival.content.MaterialType.TALLOW))).save(writer);
        // The modern builder rejects a 1x1 shaped recipe; one shapeless ingredient has identical matching.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,TCItems.material(tc4revival.content.MaterialType.TALLOW),9)
                .requires(tc4revival.content.TCCleansing.TALLOW_BLOCK.get()).unlockedBy("has_tallow_block",has(tc4revival.content.TCCleansing.TALLOW_BLOCK.get())).save(writer,"thaumcraft:tallow_from_block");
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,tc4revival.content.TCEldritch.OBSIDIAN_TILE.get(),4)
                .pattern("##").pattern("##").define('#',Items.OBSIDIAN).unlockedBy("has_obsidian",has(Items.OBSIDIAN)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,TCItems.candle(0),3).pattern("S").pattern("T").pattern("T").define('S',Items.STRING).define('T',TCItems.material(tc4revival.content.MaterialType.TALLOW)).unlockedBy("has_tallow",has(TCItems.material(tc4revival.content.MaterialType.TALLOW))).save(writer,"thaumcraft:tallow_candle");
        for (int i=1;i<16;i++) ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS,TCItems.candle(i)).requires(TCItems.candle(0)).requires(ItemTags.create(ResourceLocation.parse("forge:dyes/"+net.minecraft.world.item.DyeColor.byId(i).getName()))).unlockedBy("has_candle",has(TCItems.candle(0))).save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS,TCItems.candle(0)).requires(ItemTags.create(ResourceLocation.parse("thaumcraft:tallow_candles"))).requires(Items.WHITE_DYE).unlockedBy("has_candle",has(TCItems.candle(0))).save(writer,"thaumcraft:tallow_candle_whiten");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,TCItems.material(tc4revival.content.MaterialType.QUICKSILVER)).requires(TCItems.plant(tc4revival.content.MagicalPlantBlock.Kind.SHIMMERLEAF))
                .unlockedBy("has_shimmerleaf",has(TCItems.plant(tc4revival.content.MagicalPlantBlock.Kind.SHIMMERLEAF))).save(writer,"thaumcraft:quicksilver_from_shimmerleaf");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,Items.BLAZE_POWDER).requires(TCItems.plant(tc4revival.content.MagicalPlantBlock.Kind.CINDERPEARL))
                .unlockedBy("has_cinderpearl",has(TCItems.plant(tc4revival.content.MagicalPlantBlock.Kind.CINDERPEARL))).save(writer,"thaumcraft:blaze_powder_from_cinderpearl");
        for (String wood:java.util.List.of("greatwood","silverwood")) {
            var log=wood.equals("greatwood") ? TCItems.GREATWOOD_LOG.get() : TCItems.SILVERWOOD_LOG.get();
            var planks=TCItems.woodwork(wood+"_planks");
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS,planks,4).requires(log).unlockedBy("has_log",has(log)).save(writer);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,TCItems.woodwork(wood+"_slab"),6).pattern("KKK").define('K',planks).unlockedBy("has_planks",has(planks)).save(writer);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,TCItems.woodwork(wood+"_stairs"),4).pattern("K  ").pattern("KK ").pattern("KKK").define('K',planks).unlockedBy("has_planks",has(planks)).save(writer);
        }
        for (var type:tc4revival.content.ShardType.values()) {
            var recipe=net.minecraft.data.recipes.ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS,TCItems.crystal(type));
            for (int index=0;index<6;index++) recipe.requires(TCItems.shard(type==tc4revival.content.ShardType.BALANCED ? tc4revival.content.ShardType.values()[index] : type));
            recipe.unlockedBy("has_shard",has(TCItems.shard(type==tc4revival.content.ShardType.BALANCED ? tc4revival.content.ShardType.AIR : type))).save(writer);
        }
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,TCItems.EMPTY_PHIAL.get(),8).pattern(" C ").pattern("G G").pattern(" G ")
                .define('C',Items.CLAY_BALL).define('G',Items.GLASS).unlockedBy("has_glass",has(Items.GLASS)).save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,TCItems.JAR_LABEL.get(),4).requires(net.minecraftforge.common.Tags.Items.DYES_BLACK).requires(Items.SLIME_BALL).requires(Items.PAPER,4)
                .unlockedBy("has_paper",has(Items.PAPER)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,TCItems.FLESH_BLOCK.get()).pattern("FFF").pattern("FFF").pattern("FFF")
                .define('F',Items.ROTTEN_FLESH).unlockedBy("has_rotten_flesh",has(Items.ROTTEN_FLESH)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TCItems.ARCANE_STONE_BRICKS.get(), 4).pattern("SS").pattern("SS")
                .define('S', TCItems.ARCANE_STONE.get()).unlockedBy("has_arcane_stone", has(TCItems.ARCANE_STONE.get())).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TCItems.ARCANE_STONE_SLAB.get(), 6).pattern("KKK")
                .define('K', TCItems.ARCANE_STONE_BRICKS.get()).unlockedBy("has_arcane_bricks", has(TCItems.ARCANE_STONE_BRICKS.get())).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TCItems.ARCANE_STONE_STAIRS.get(), 4).pattern("K  ").pattern("KK ").pattern("KKK")
                .define('K', TCItems.ARCANE_STONE_BRICKS.get()).unlockedBy("has_arcane_bricks", has(TCItems.ARCANE_STONE_BRICKS.get())).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, TCItems.CASTING_WAND.get())
                .pattern("  I").pattern(" S ").pattern("I  ")
                .define('I', TCItems.wandComponent("wand_cap_iron")).define('S', net.minecraftforge.common.Tags.Items.RODS_WOODEN)
                .unlockedBy("has_cap", has(TCItems.wandComponent("wand_cap_iron"))).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TCItems.wandComponent("wand_cap_iron"))
                .pattern("NNN").pattern("N N").define('N', ItemTags.create(ResourceLocation.parse("forge:nuggets/iron")))
                .unlockedBy("has_iron", has(Items.IRON_NUGGET)).save(writer);
        var thaumium = ItemTags.create(ResourceLocation.parse("forge:ingots/thaumium"));
        var voidMetal = ItemTags.create(ResourceLocation.parse("forge:ingots/void"));
        for (var type : tc4revival.content.VoidEquipment.Tool.values()) {
            var recipe = ShapedRecipeBuilder.shaped(type == tc4revival.content.VoidEquipment.Tool.SWORD ? RecipeCategory.COMBAT : RecipeCategory.TOOLS, TCItems.voidTool(type))
                    .define('I', voidMetal).define('S', net.minecraftforge.common.Tags.Items.RODS_WOODEN)
                    .unlockedBy("has_void_metal", has(voidMetal));
            type.pattern.forEach(recipe::pattern); recipe.save(writer);
        }
        for (var type : tc4revival.content.VoidEquipment.Armor.values()) {
            var recipe = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, TCItems.voidArmor(type))
                    .define('I', voidMetal).unlockedBy("has_void_metal", has(voidMetal));
            type.pattern.forEach(recipe::pattern); recipe.save(writer);
        }
        for (var type : tc4revival.content.ThaumiumToolType.values()) {
            var recipe = ShapedRecipeBuilder.shaped(type == tc4revival.content.ThaumiumToolType.SWORD ? RecipeCategory.COMBAT : RecipeCategory.TOOLS, TCItems.tool(type))
                    .define('I', thaumium).define('S', net.minecraftforge.common.Tags.Items.RODS_WOODEN)
                    .unlockedBy("has_thaumium", has(thaumium));
            type.pattern().forEach(recipe::pattern); recipe.save(writer);
        }
        for (var type : tc4revival.content.ThaumiumArmorType.values()) {
            var recipe = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, TCItems.armor(type))
                    .define('I', thaumium).unlockedBy("has_thaumium", has(thaumium));
            type.pattern().forEach(recipe::pattern);
            recipe.save(writer);
        }
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TCItems.bauble(tc4revival.content.MundaneBaubleType.AMULET))
                .pattern(" S ").pattern("S S").pattern(" I ").define('S', Items.STRING).define('I', Items.GOLD_INGOT)
                .unlockedBy("has_gold", has(Items.GOLD_INGOT)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TCItems.bauble(tc4revival.content.MundaneBaubleType.RING))
                .pattern(" N ").pattern("N N").pattern(" N ").define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_gold", has(Items.GOLD_NUGGET)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TCItems.bauble(tc4revival.content.MundaneBaubleType.BELT))
                .pattern(" L ").pattern("L L").pattern(" I ").define('L', Items.LEATHER).define('I', Items.GOLD_INGOT)
                .unlockedBy("has_gold", has(Items.GOLD_INGOT)).save(writer);
        var meats = java.util.List.of(tc4revival.content.FoodType.CHICKEN, tc4revival.content.FoodType.BEEF,
                tc4revival.content.FoodType.PORK, tc4revival.content.FoodType.FISH);
        for (var omitted : meats) {
            var recipe = ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, TCItems.food(tc4revival.content.FoodType.TRIPLE_MEAT_TREAT))
                    .requires(Items.SUGAR).unlockedBy("has_sugar", has(Items.SUGAR));
            for (var meat : meats) if (meat != omitted) recipe.requires(TCItems.food(meat));
            recipe.save(writer, ResourceLocation.parse("thaumcraft:triple_meat_treat_without_" + omitted.path()));
        }
        for (var type : tc4revival.content.ClusterType.values())
            smelt(writer, TCItems.cluster(type), type.output(), 2, 2.0f, type.path() + "_smelting");
        smelt(writer, TCItems.shard(tc4revival.content.ShardType.BALANCED), TCItems.material(tc4revival.content.MaterialType.SALIS_MUNDUS),
                1, 1.0f, "salis_mundus_smelting");
        smelt(writer, TCItems.material(tc4revival.content.MaterialType.GOLD_COIN), Items.GOLD_NUGGET, 1, 0.0f, "gold_coin_smelting");
        for (var type : tc4revival.content.NuggetType.values()) {
            var materialIngredient = type == tc4revival.content.NuggetType.COPPER
                    ? net.minecraft.world.item.crafting.Ingredient.of(net.minecraftforge.common.Tags.Items.INGOTS_COPPER)
                    : net.minecraft.world.item.crafting.Ingredient.of(type.materialItem());
            // One occupied cell has identical placement semantics in either recipe form.
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TCItems.nugget(type), 9)
                    .requires(materialIngredient)
                    .unlockedBy("has_material", type == tc4revival.content.NuggetType.COPPER
                            ? has(net.minecraftforge.common.Tags.Items.INGOTS_COPPER) : has(type.materialItem()))
                    .save(writer, ResourceLocation.parse("thaumcraft:" + type.path() + "_from_material"));
            // Old ShapedOreRecipe used an explicit ItemStack here, not an ore name.
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, type.materialItem())
                    .pattern("NNN").pattern("NNN").pattern("NNN").define('N', TCItems.nugget(type))
                    .unlockedBy("has_nugget", has(TCItems.nugget(type)))
                    .save(writer, ResourceLocation.parse("thaumcraft:" + type.materialPath() + "_from_nuggets"));
        }
        var shards = ItemTags.create(ResourceLocation.parse("thaumcraft:shards"));
        var black = net.minecraftforge.common.crafting.CompoundIngredient.of(
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraftforge.common.Tags.Items.DYES_BLACK),
                net.minecraft.world.item.crafting.Ingredient.of(Items.INK_SAC)); // The old dyeBlack material was the ink sac itself.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TCItems.TABLE.get())
                .pattern("SSS").pattern("W W").define('S', ItemTags.WOODEN_SLABS).define('W', ItemTags.PLANKS)
                .unlockedBy("has_planks", has(ItemTags.PLANKS)).save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, TCItems.SCRIBING_TOOLS.get())
                .requires(Items.GLASS_BOTTLE).requires(Items.FEATHER).requires(black)
                .unlockedBy("has_bottle", has(Items.GLASS_BOTTLE)).save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, TCItems.SCRIBING_TOOLS.get())
                .requires(TCItems.SCRIBING_TOOLS.get()).requires(black)
                .unlockedBy("has_tools", has(TCItems.SCRIBING_TOOLS.get())).save(writer, ResourceLocation.parse("thaumcraft:refill_scribing_tools"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TCItems.UNKNOWN_RESEARCH_NOTES.get())
                .pattern("KKK").pattern("KKK").pattern("KKK").define('K', TCItems.KNOWLEDGE_FRAGMENT.get())
                .unlockedBy("has_fragment", has(TCItems.KNOWLEDGE_FRAGMENT.get())).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, TCItems.THAUMOMETER.get())
                .pattern(" S ").pattern("GCG").pattern(" S ")
                .define('S', shards).define('G', Items.GOLD_INGOT).define('C', Items.GLASS)
                .unlockedBy("has_shard", has(shards)).save(writer);
    }
    private static void smelt(Consumer<FinishedRecipe> writer, net.minecraft.world.item.Item input, net.minecraft.world.item.Item output,
                              int count, float experience, String path) {
        // Forge's vanilla cooking serializer accepts an ItemStack result, but the vanilla builder exposes only one item.
        net.minecraft.data.recipes.SimpleCookingRecipeBuilder.smelting(net.minecraft.world.item.crafting.Ingredient.of(input),
                RecipeCategory.MISC, output, experience, 200).unlockedBy("has_input", has(input)).save(recipe -> writer.accept(new FinishedRecipe() {
                    @Override public void serializeRecipeData(com.google.gson.JsonObject json) {
                        recipe.serializeRecipeData(json);
                        var result = new com.google.gson.JsonObject();
                        result.addProperty("item", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output).toString());
                        result.addProperty("count", count); json.add("result", result);
                    }
                    @Override public ResourceLocation getId() { return recipe.getId(); }
                    @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() { return recipe.getType(); }
                    @Override public com.google.gson.JsonObject serializeAdvancement() { return recipe.serializeAdvancement(); }
                    @Override public ResourceLocation getAdvancementId() { return recipe.getAdvancementId(); }
                }), ResourceLocation.parse("thaumcraft:" + path));
    }
}
