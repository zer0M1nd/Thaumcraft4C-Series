package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.*;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.function.Consumer;

public final class ArcaneGTRecipes {
    public static void generate(Consumer<FinishedRecipe> provider){
        fluidsAndWashing(provider);
        for(var material:GTCEuAPI.materialManager.getRegisteredMaterials())if(material.hasProperty(PropertyKey.ORE))cluster(provider,material);
        for(var fuel:EssentiaFuelValues.ENERGY.entrySet())if(tc4revival.aspect.TCAspects.catalog().find(fuel.getKey()).isPresent())ArcaneMachines.FUELS.recipeBuilder(ArcaneGT.id("gt/fuels/"+fuel.getKey()))
                .input(EssentiaFuelCapability.CAP,new EssentiaFuel(fuel.getKey(),4)).inputFluids(GTMaterials.Lubricant.getFluid(1))
                .duration(EssentiaFuelValues.ticks(fuel.getValue())).EUt(-4096).save(provider);
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(ArcaneGT.id("gt/arcane_machine_casing"))
                .inputItems(TagPrefix.plate,ArcaneMaterials.THAUMIUM,6).inputItems(TagPrefix.frameGt,GTMaterials.Titanium)
                .inputItems(item("thaumcraft:vis_filter")).outputItems(ArcaneMachines.CASING.asStack(2)).duration(200).EUt(1920).save(provider);
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(ArcaneGT.id("gt/large_essentia_generator"))
                .inputItems(ArcaneMachines.CASING.asStack(2)).inputItems(item("gtceu:ev_machine_hull"))
                .inputItems(item("gtceu:ev_electric_pump"),2).inputItems(item("gtceu:ev_emitter"),2)
                .inputItems(ItemTags.create(ResourceLocation.parse("gtceu:circuits/ev")),2)
                .inputItems(item("thaumcraft:essentia_valve"),2).inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                .outputItems(ArcaneMachines.GENERATOR.asStack()).duration(600).EUt(1920).save(provider);
        for(int tier=1;tier<ArcaneMachines.HATCHES.length;tier++){
            var hatch=ArcaneMachines.HATCHES[tier];if(hatch==null)continue;
            var fluidHatch=GTMachines.FLUID_IMPORT_HATCH[tier];
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(ArcaneGT.id("gt/"+hatch.getId().getPath()))
                    .inputItems(fluidHatch!=null?fluidHatch.asStack():ArcaneMachines.HATCHES[tier-1].asStack(2))
                    .inputItems(item("thaumcraft:essentia_valve"),tier).inputItems(item("thaumcraft:vis_filter"),tier)
                    .inputItems(TagPrefix.plate,ArcaneMaterials.THAUMIUM,2*tier).outputItems(hatch.asStack()).duration(100)
                    .EUt(com.gregtechceu.gtceu.api.GTValues.VA[Math.min(tier,4)]).save(provider);
        }
    }
    private static void fluidsAndWashing(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.FLUID_SOLIDFICATION_RECIPES.recipeBuilder(ArcaneGT.id("gt/solidify_quicksilver"))
                .notConsumable(GTItems.SHAPE_MOLD_BALL).inputFluids(GTMaterials.Mercury.getFluid(1000))
                .outputItems(item("thaumcraft:quicksilver")).duration(200).EUt(30).save(provider);
        GTRecipeTypes.EXTRACTOR_RECIPES.recipeBuilder(ArcaneGT.id("gt/extract_quicksilver"))
                .inputItems(item("thaumcraft:quicksilver")).outputFluids(GTMaterials.Mercury.getFluid(1000)).duration(200).EUt(30).save(provider);
        for(String name:List.of("liquid_death","purifying_water")) {
            var fluid=ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("thaumcraft:"+name));
            GTRecipeTypes.CENTRIFUGE_RECIPES.recipeBuilder(ArcaneGT.id("gt/centrifuge_"+name+"_bucket"))
                    .inputItems(item("thaumcraft:"+name+"_bucket")).outputItems(Items.BUCKET)
                    .outputFluids(new net.minecraftforge.fluids.FluidStack(fluid,1000)).duration(200).EUt(30).save(provider);
        }
        GTRecipeTypes.MIXER_RECIPES.recipeBuilder(ArcaneGT.id("gt/mix_purifying_water"))
                .inputFluids(GTMaterials.Water.getFluid(1000)).inputItems(tc4revival.content.TCCleansing.BATH_SALTS.get())
                .outputFluids(new net.minecraftforge.fluids.FluidStack(tc4revival.content.TCFluids.PURE.get(),1000)).duration(200).EUt(30).save(provider);
        GTRecipeTypes.CHEMICAL_BATH_RECIPES.recipeBuilder(ArcaneGT.id("gt/bathe_amber_liquid_death"))
                .category(com.gregtechceu.gtceu.common.data.GTRecipeCategories.ORE_BATHING)
                .inputItems(TagPrefix.crushed,ArcaneMaterials.AMBER).inputFluids(com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient.of(tc4revival.content.TCFluids.DEATH.get(),10))
                .outputItems(TagPrefix.crushedPurified,ArcaneMaterials.AMBER).chancedOutput(new ItemStack(item("thaumcraft:magic_tallow")),7000,0)
                .chancedOutput(TagPrefix.dust,GTMaterials.Stone,4000,0).duration(200).EUt(30).save(provider);
        for(var entry:ArcaneMaterials.INFUSED.entrySet()) {
            var aspect=tc4revival.aspect.TCAspects.catalog().require(List.of("aer","ignis","aqua","terra","ordo","perditio").get(entry.getKey().metadata()));
            GTRecipeTypes.CHEMICAL_BATH_RECIPES.recipeBuilder(ArcaneGT.id("gt/bathe_"+entry.getValue().getName()+"_purifying_water"))
                    .category(com.gregtechceu.gtceu.common.data.GTRecipeCategories.ORE_BATHING)
                    .inputItems(TagPrefix.crushed,entry.getValue()).inputFluids(com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient.of(tc4revival.content.TCFluids.PURE.get(),100))
                    .outputItems(TagPrefix.crushedPurified,entry.getValue()).chancedOutput(tc4revival.content.TCItems.WISP_ESSENCE.get().of(aspect),7000,0)
                    .chancedOutput(TagPrefix.dust,GTMaterials.Stone,4000,0).duration(200).EUt(30).save(provider);
        }
    }
    private static void cluster(Consumer<FinishedRecipe> provider,Material material){
        var cluster=ChemicalHelper.get(ArcaneGT.CLUSTER,material);if(cluster.isEmpty())return;
        var ore=material.getProperty(PropertyKey.ORE);var smelt=ore.getDirectSmeltResult().isNull()?material:ore.getDirectSmeltResult();
        var output=ChemicalHelper.get(smelt.hasProperty(PropertyKey.INGOT)?TagPrefix.ingot:smelt.hasProperty(PropertyKey.GEM)?TagPrefix.gem:TagPrefix.dust,smelt,ore.getOreMultiplier()*2);
        var key=material.getModid()+"/"+material.getName();
        if(!output.isEmpty())SimpleCookingRecipeBuilder.smelting(Ingredient.of(cluster),RecipeCategory.MISC,output.getItem(),.7f,200)
                .unlockedBy("has_cluster",net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(cluster.getItem()))
                .save(counted(provider,output.getCount()),ArcaneGT.id("gt/clusters/smelting/"+key));
        var crushed=ChemicalHelper.get(TagPrefix.crushed,material,ore.getOreMultiplier()*3);
        if(!crushed.isEmpty())GTRecipeTypes.MACERATOR_RECIPES.recipeBuilder(ArcaneGT.id("gt/clusters/macerating/"+key))
                .inputItems(cluster).outputItems(crushed).duration(400).EUt(2).save(provider);
    }
    /** Vanilla's builder accepts a result item only; retain the required doubled output count. */
    private static Consumer<FinishedRecipe> counted(Consumer<FinishedRecipe> provider,int count){return recipe->provider.accept(new FinishedRecipe(){
        public void serializeRecipeData(com.google.gson.JsonObject json){recipe.serializeRecipeData(json);var result=new com.google.gson.JsonObject();result.add("item",json.get("result"));result.addProperty("count",count);json.add("result",result);}
        public ResourceLocation getId(){return recipe.getId();}
        public net.minecraft.world.item.crafting.RecipeSerializer<?> getType(){return recipe.getType();}
        public com.google.gson.JsonObject serializeAdvancement(){return recipe.serializeAdvancement();}
        public ResourceLocation getAdvancementId(){return recipe.getAdvancementId();}
    });}
    private static Item item(String id){var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));if(item==null||item==Items.AIR)throw new IllegalArgumentException("Missing GT recipe ingredient: "+id);return item;}
    private ArcaneGTRecipes(){}
}
