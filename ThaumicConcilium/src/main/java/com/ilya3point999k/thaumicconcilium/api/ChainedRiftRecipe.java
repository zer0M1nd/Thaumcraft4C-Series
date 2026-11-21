package com.ilya3point999k.thaumicconcilium.api;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.AspectList;

/** Modern source-level facade for the original API; constructors own their mutable inputs. */
public final class ChainedRiftRecipe {
    public final String key;
    public final Ingredient catalyst;
    public final AspectList aspects;
    public final int hash;
    public final int catalystCount;
    private final ItemStack output;
    public ChainedRiftRecipe(String researchKey,ItemStack result,Object material,AspectList tags){
        this(researchKey,result,material,tags,1);
    }
    ChainedRiftRecipe(String researchKey,ItemStack result,Object material,AspectList tags,int count){
        if(count<1||count>64)throw new IllegalArgumentException("Catalyst count must be 1..64");catalystCount=count;
        key=Objects.requireNonNull(researchKey);output=Objects.requireNonNull(result).copy();aspects=Objects.requireNonNull(tags).copy();catalyst=ingredient(material);
        if(output.isEmpty()||aspects.size()==0)throw new IllegalArgumentException("A chained rift recipe requires output and essentia");
        for(var aspect:aspects.getAspects())if(aspects.getAmount(aspect)<=0)throw new IllegalArgumentException("Essentia costs must be positive");
        hash=toJson().toString().hashCode();
    }
    public static Ingredient ingredient(Object material){
        if(material instanceof Ingredient ingredient)return ingredient;
        if(material instanceof ItemStack stack){if(stack.isEmpty())throw new IllegalArgumentException("Empty catalyst");return stack.hasTag()?net.minecraftforge.common.crafting.StrictNBTIngredient.of(stack.copyWithCount(1)):Ingredient.of(stack);}
        if(material instanceof net.minecraft.world.level.ItemLike item)return Ingredient.of(item);
        if(material instanceof String text){var tag=text.startsWith("#")?text.substring(1):text;var id=tag.indexOf(':')<1?null:ResourceLocation.tryParse(tag);if(id==null)throw new IllegalArgumentException("Use a namespaced modern item tag: "+text);return Ingredient.of(TagKey.create(Registries.ITEM,id));}
        if(material instanceof java.util.Collection<?> list){var alternatives=list.stream().map(value->{if(!(value instanceof ItemStack stack)||stack.isEmpty())throw new IllegalArgumentException("Invalid catalyst alternative");return ingredient(stack);}).toArray(Ingredient[]::new);if(alternatives.length==0)throw new IllegalArgumentException("Empty catalyst alternatives");return net.minecraftforge.common.crafting.CompoundIngredient.of(alternatives);}
        throw new IllegalArgumentException("Unsupported catalyst "+material);
    }
    public boolean catalystMatches(ItemStack stack){return !stack.isEmpty()&&catalyst.test(stack);}
    public boolean matches(AspectList stored,ItemStack stack){if(stored==null||stack.getCount()<catalystCount||!catalystMatches(stack))return false;for(var aspect:aspects.getAspects())if(stored.getAmount(aspect)<aspects.getAmount(aspect))return false;return true;}
    public AspectList removeMatching(AspectList stored){var remaining=stored.copy();for(var aspect:aspects.getAspects())remaining.remove(aspect,aspects.getAmount(aspect));return remaining;}
    public ItemStack getRecipeOutput(){return output.copy();}
    public JsonObject toJson(){
        var json=new JsonObject();json.addProperty("type","thaumicconcilium:chained_rift");json.addProperty("research",key);json.add("catalyst",catalyst.toJson());json.addProperty("count",catalystCount);
        var costs=new JsonObject();Arrays.stream(aspects.getAspects()).sorted(Comparator.comparing(a->a.getTag())).forEach(a->costs.addProperty(a.getTag(),aspects.getAmount(a)));json.add("aspects",costs);
        var result=new JsonObject();result.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output.getItem()).toString());result.addProperty("count",output.getCount());if(output.hasTag())result.addProperty("nbt",output.getTag().toString());json.add("result",result);return json;
    }
    @Override public boolean equals(Object value){return value instanceof ChainedRiftRecipe recipe&&toJson().equals(recipe.toJson());}
    @Override public int hashCode(){return toJson().hashCode();}
}
