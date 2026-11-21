package thaumicomniverse.research;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicomniverse.compat.ProjectMods;

/** All registry-driven third-party infusion enchantments share one research gate. */
public class UniversalEnchantments {
    public static final String RESEARCH="thaumicomniverse:UNIVERSAL_ENCHANTMENTS";
    /** Retained only for recognizing completed research from earlier saves. */
    public static final String PREFIX="thaumicomniverse:ENCHANT/";
    public static List<ResourceLocation> enchantments() {
        return ForgeRegistries.ENCHANTMENTS.getKeys().stream()
                .filter(id->!id.getNamespace().equals("minecraft")&&!ProjectMods.NAMESPACES.contains(id.getNamespace())).sorted().toList();
    }
    public static String key(ResourceLocation id) { return PREFIX+id.getNamespace()+"/"+id.getPath(); }
    public static ResourceLocation recipeId(ResourceLocation id) { return ResourceLocation.parse("thaumicomniverse:enchantment/"+id.getNamespace()+"/"+id.getPath()); }
    public static JsonObject definition(ResourceLocation id) {
        var enchantment=Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getValue(id));
        int rarity=switch(enchantment.getRarity()) {case COMMON->1;case UNCOMMON->2;case RARE->3;case VERY_RARE->4;};
        var json=new JsonObject();json.addProperty("type","thaumcraft:infusion");json.addProperty("research",RESEARCH);json.addProperty("enchantment",id.toString());json.addProperty("instability",2+rarity);json.addProperty("base_xp",2+rarity*2);
        var aspects=new JsonObject();aspects.addProperty("praecantatio",8+4*rarity);aspects.addProperty("cognitio",4+4*rarity);
        String aspect="instrumentum",item="minecraft:quartz";
        if(enchantment.isCurse()){aspect="alienis";item="minecraft:fermented_spider_eye";}
        else if(enchantment.category==EnchantmentCategory.WEAPON||enchantment.category==EnchantmentCategory.BOW||enchantment.category==EnchantmentCategory.CROSSBOW||enchantment.category==EnchantmentCategory.TRIDENT){aspect="telum";item="minecraft:iron_sword";}
        else if(enchantment.category==EnchantmentCategory.ARMOR||enchantment.category==EnchantmentCategory.ARMOR_HEAD||enchantment.category==EnchantmentCategory.ARMOR_CHEST||enchantment.category==EnchantmentCategory.ARMOR_LEGS||enchantment.category==EnchantmentCategory.ARMOR_FEET){aspect="tutamen";item="minecraft:iron_ingot";}
        else if(enchantment.category==EnchantmentCategory.DIGGER){aspect="perfodio";item="minecraft:iron_pickaxe";}
        else if(enchantment.category==EnchantmentCategory.FISHING_ROD){aspect="aqua";item="minecraft:prismarine_shard";}
        aspects.addProperty(aspect,8+4*rarity);json.add("aspects",aspects);
        var components=new JsonArray();
        for(String material:List.of("minecraft:book","minecraft:lapis_lazuli",item,rarity>=3?"minecraft:diamond":"minecraft:quartz","thaumcraft:salis_mundus")) {
            var ingredient=new JsonObject();ingredient.addProperty("item",material);var component=new JsonObject();component.add("ingredient",ingredient);components.add(component);
        }
        json.add("components",components);return json;
    }
    public static void recipes(RecipeManager manager) {
        if(!thaumicomniverse.RevivalFeatures.Group.ENCHANTMENTS.enabled())return;
        var all=new ArrayList<Recipe<?>>(manager.getRecipes());boolean changed=false;
        for(var id:enchantments()) {
            var recipe=recipeId(id);if(manager.byKey(recipe).isPresent())continue;
            all.add(tc4revival.content.TCArcane.INFUSION_RECIPE_SERIALIZER.get().fromJson(recipe,definition(id)));changed=true;
        }
        if(changed)manager.replaceRecipes(all);
    }
    public static void research(String folder,JsonArray result) {
        if(!folder.equals("research_pages"))return;
        appendPages(result,enchantments());
    }

    /** One stable recipe component per enchantment; the map still contains exactly one node. */
    public static void appendPages(JsonArray result,List<ResourceLocation> enchantments) {
        for(var element:result) {
            var row=element.getAsJsonObject();
            if(!RESEARCH.equals(row.get("key").getAsString()))continue;
            var pages=row.getAsJsonArray("pages");
            // Safe if the assembled data is transformed again; authored components are preserved.
            for(int i=pages.size()-1;i>=0;i--) {
                var page=pages.get(i).getAsJsonObject();
                if(page.has("id")&&page.get("id").getAsString().startsWith(RESEARCH+"/recipe/"))pages.remove(i);
            }
            for(var id:enchantments.stream().distinct().sorted().toList()) {
                var recipe=new JsonObject();recipe.addProperty("kind","reference");recipe.addProperty("class","modern");
                var keys=new JsonArray();keys.add(recipeId(id).toString());recipe.add("keys",keys);
                var page=new JsonObject();page.addProperty("type","INFUSION_ENCHANTMENT");
                page.addProperty("id",RESEARCH+"/recipe/"+id.getNamespace()+"/"+id.getPath());
                page.add("recipe",recipe);pages.add(page);
            }
        }
    }
}
