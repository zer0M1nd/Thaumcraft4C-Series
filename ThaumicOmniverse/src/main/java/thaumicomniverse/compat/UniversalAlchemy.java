package thaumicomniverse.compat;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicomniverse.content.UniversalClusterItem;

public class UniversalAlchemy {
    public static final String CLUSTERS="thaumicomniverse:UNIVERSAL_CLUSTERS",TRANSMUTATION="thaumicomniverse:UNIVERSAL_TRANSMUTATION";
    private static final Map<String,String> AFFINITIES=loadAffinities();
    private static Map<String,String> loadAffinities() {
        try(var stream=UniversalAlchemy.class.getResourceAsStream("/assets/thaumicomniverse/material_affinities.json");var reader=new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)) {
            var result=new TreeMap<String,String>();JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsString()));return Map.copyOf(result);
        }catch(IOException error){throw new UncheckedIOException(error);}
    }
    public static String affinity(String material){return AFFINITIES.getOrDefault(material,"fabrico");}
    public static ItemStack representative(String tag) {
        var values=ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(ResourceLocation.parse(tag))).stream()
                .sorted(Comparator.comparingInt((Item item)->priority(ForgeRegistries.ITEMS.getKey(item).getNamespace())).thenComparing(item->ForgeRegistries.ITEMS.getKey(item).toString())).toList();
        return values.isEmpty()?ItemStack.EMPTY:new ItemStack(values.get(0));
    }
    private static int priority(String namespace){return namespace.equals("minecraft")?0:namespace.equals("thaumcraft")?1:namespace.equals("gtceu")?2:3;}
    private static ItemStack cluster(String material) {
        if(net.minecraftforge.fml.ModList.get().isLoaded("gtceu")) {
            var nativeForm=thaumicomniverse.compat.gregtech.ArcaneGT.cluster(material);
            if(!nativeForm.isEmpty())return nativeForm;
        }
        return representative("forge:clusters/"+material);
    }
    private static ItemStack smeltProduct(RecipeManager manager,RegistryAccess registries,ItemStack ore) {
        for(var recipe:manager.getAllRecipesFor(RecipeType.SMELTING))if(recipe.getIngredients().get(0).test(ore))return recipe.getResultItem(registries).copy();
        return ItemStack.EMPTY;
    }
    public static ItemStack product(RecipeManager manager,RegistryAccess registries,String material,ItemStack ore) {
        var result=smeltProduct(manager,registries,ore);if(!result.isEmpty())return result;
        for(String form:List.of("ingots","gems","dusts","raw_materials")){result=representative("forge:"+form+"/"+material);if(!result.isEmpty())return result;}
        return ItemStack.EMPTY;
    }
    /** Some ores only drop shards and have neither a furnace recipe nor a conventional material tag. */
    public static void completeOreRecipes(net.minecraft.server.level.ServerLevel level) {
        if(!thaumicomniverse.RevivalFeatures.Group.UNIVERSAL.enabled())return;
        var manager=level.getRecipeManager();
        var all=new LinkedHashMap<ResourceLocation,Recipe<?>>();manager.getRecipes().forEach(recipe->all.put(recipe.getId(),recipe));
        int before=all.size();
        var tags=ForgeRegistries.ITEMS.tags().getTagNames().map(tag->tag.location())
                .filter(tag->tag.getNamespace().equals("forge")&&tag.getPath().startsWith("ores/")&&!tag.getPath().substring(5).contains("/"))
                .sorted().toList();
        for(var tag:tags) {
            String material=tag.getPath().substring(5);
            if(all.containsKey(ResourceLocation.parse("thaumicomniverse:universal/clusters/"+material)))continue;
            var ore=representative(tag.toString());if(!(ore.getItem() instanceof BlockItem block))continue;
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,block.getBlock().defaultBlockState())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,new ItemStack(Items.NETHERITE_PICKAXE))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            var table=level.getServer().getLootData().getLootTable(block.getBlock().getLootTable());
            // Fixed seed, no silk touch/fortune, and no self-ore output that could form a duplication loop.
            var drops=table.getRandomItems(params,1L);
            var product=drops.stream().filter(stack->!stack.isEmpty()&&!stack.is(ore.getItem())
                    &&!stack.is(ItemTags.create(ResourceLocation.parse("forge:ores"))))
                    .max(Comparator.comparingInt(ItemStack::getCount)).map(stack->stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
            if(!product.isEmpty())addClusterRecipes(all,material,tag.toString(),product);
        }
        if(all.size()!=before)manager.replaceRecipes(all.values());
    }

    private static void addClusterRecipes(Map<ResourceLocation,Recipe<?>> all,String material,String oreTag,ItemStack product) {
        var cluster=cluster(material);boolean generic=cluster.isEmpty();
        if(generic)cluster=UniversalClusterItem.of(material,product);
        // One ore block follows TC4's two-cluster yield. Each raw ore yields one cluster.
        addCrucible(all,"clusters/"+material,CLUSTERS,oreTag,cluster.copyWithCount(2),Map.of("metallum",2,"ordo",2));
        var raw=representative("forge:raw_materials/"+material);
        if(!raw.isEmpty())addCrucible(all,"clusters/raw/"+material,CLUSTERS,"forge:raw_materials/"+material,cluster,Map.of("metallum",2,"ordo",2));
        if(generic) {
            var ingredient=net.minecraftforge.common.crafting.StrictNBTIngredient.of(cluster.copyWithCount(1));
            var id=ResourceLocation.parse("thaumicomniverse:universal/smelting/"+material);
            all.put(id,new SmeltingRecipe(id,"thaumicomniverse:clusters",CookingBookCategory.MISC,ingredient,product.copyWithCount(Math.min(product.getMaxStackSize(),product.getCount()*2)),.5f,200));
        }
    }
    public static void recipes(RecipeManager manager,RegistryAccess registries) {
        if(!thaumicomniverse.RevivalFeatures.Group.UNIVERSAL.enabled())return;
        var all=new LinkedHashMap<ResourceLocation,Recipe<?>>();manager.getRecipes().forEach(recipe->all.put(recipe.getId(),recipe));
        var tags=ForgeRegistries.ITEMS.tags().getTagNames().map(tag->tag.location()).filter(tag->tag.getNamespace().equals("forge")).sorted().toList();
        for(var tag:tags) {
            String path=tag.getPath();if(!path.startsWith("ores/")||path.substring(5).contains("/"))continue;
            String material=path.substring(5);var ore=representative(tag.toString());if(ore.isEmpty())continue;
            var product=product(manager,registries,material,ore);if(product.isEmpty())continue;
            addClusterRecipes(all,material,tag.toString(),product);
        }
        for(var tag:tags) {
            String path=tag.getPath();if(!path.startsWith("ingots/")||path.substring(7).contains("/"))continue;
            String material=path.substring(7);if(Set.of("brick","graphite","hop_graphite","insulating_resin","sky_insulating_resin","netherite_scrap","obsidian","borosilicate_glass","graphene","polyvinyl_butyral","wax",
                    "clay","epoxy","nether_brick","polybenzimidazole","polycaprolactam","polyethylene","polyphenylene_sulfide","polytetrafluoroethylene","polyvinyl_chloride","raw_rubber","reinforced_epoxy_resin","rubber","silicone_rubber","styrene_butadiene_rubber").contains(material))continue;
            var ingot=representative(tag.toString());if(ingot.isEmpty())continue;
            String special=affinity(material);var nugget=representative("forge:nuggets/"+material);
            addCrucible(all,"transmutation/"+material,TRANSMUTATION,tag.toString(),ingot.copyWithCount(3),Map.of("metallum",18,special,9));
            if(!nugget.isEmpty())addCrucible(all,"transmutation/nugget/"+material,TRANSMUTATION,"forge:nuggets/"+material,nugget.copyWithCount(3),Map.of("metallum",2,special,1));
        }
        manager.replaceRecipes(all.values());
    }
    private static JsonObject stack(ItemStack stack) {
        var result=new JsonObject();result.addProperty("item",ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());result.addProperty("count",stack.getCount());
        if(stack.hasTag())result.addProperty("nbt",stack.getTag().toString());return result;
    }
    private static void addCrucible(Map<ResourceLocation,Recipe<?>> all,String name,String research,String tag,ItemStack output,Map<String,Integer> amounts) {
        var id=ResourceLocation.parse("thaumicomniverse:universal/"+name);if(all.containsKey(id))return;
        var json=new JsonObject();json.addProperty("type","thaumcraft:crucible");json.addProperty("research",research);
        var catalyst=new JsonObject();catalyst.addProperty("tag",tag);json.add("catalyst",catalyst);json.add("result",stack(output));
        var costs=new JsonObject();amounts.forEach(costs::addProperty);json.add("aspects",costs);
        all.put(id,tc4revival.content.TCArcane.CRUCIBLE_RECIPE_SERIALIZER.get().fromJson(id,json));
    }
}
