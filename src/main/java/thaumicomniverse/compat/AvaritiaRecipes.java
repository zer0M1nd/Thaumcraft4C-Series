package thaumicomniverse.compat;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicomniverse.RevivalFeatures;

/** Native extreme crafting recipes for the four Forge 1.20.1 implementations. */
public final class AvaritiaRecipes {
    private static final ResourceLocation WAND=id("thaumicomniverse:collab/infinity_wand");
    private record CatalystAddition(Ingredient added,Ingredient replaced) {}
    private static final Map<Recipe<?>,CatalystAddition> ADDED=Collections.synchronizedMap(new WeakHashMap<>());
    private AvaritiaRecipes() {}
    private static ResourceLocation id(String value){return ResourceLocation.parse(value);}
    private static boolean serializer(String value){return ForgeRegistries.RECIPE_SERIALIZERS.containsKey(id(value));}
    private static String item(String... values){
        for(String value:values)if(ForgeRegistries.ITEMS.containsKey(id(value)))return value;
        throw new IllegalStateException("Avaritia material missing: "+Arrays.toString(values));
    }
    public static Map<ResourceLocation,JsonElement> rewrite(Map<ResourceLocation,JsonElement> source) {
        if(!source.containsKey(WAND))return source;
        var result=new LinkedHashMap<>(source);
        var template=result.remove(WAND).getAsJsonObject();
        if(serializer("avaritia:shaped_table"))result.put(WAND,wandJson(template,"avaritia:shaped_table","avaritia"));
        else if(serializer("avaritia:extreme_shaped"))result.put(WAND,wandJson(template,"avaritia:extreme_shaped","avaritia"));
        else if(serializer("avaritia:extreme_crafting_table_jei"))result.put(WAND,wandJson(template,"avaritia:extreme_crafting_table_jei","avaritia"));
        if(serializer("endless:extreme_craft"))
            result.put(result.containsKey(WAND)?id(WAND+"_endless"):WAND,wandJson(template,"endless:extreme_craft","endless"));
        return result;
    }
    private static JsonObject wandJson(JsonObject template,String type,String namespace){
        var row=template.deepCopy();row.addProperty("type",type);
        var condition=new JsonObject();condition.addProperty("type","forge:mod_loaded");condition.addProperty("modid",namespace);
        var conditions=new JsonArray();conditions.add(condition);row.add("conditions",conditions);
        var key=row.getAsJsonObject("key");
        key.getAsJsonObject("I").addProperty("item",item(namespace+":infinity_ingot"));
        key.getAsJsonObject("C").addProperty("item",item(namespace+":crystal_matrix_ingot"));
        key.getAsJsonObject("N").addProperty("item",item(namespace+":neutron_ingot",namespace+":neutronium_ingot"));
        key.getAsJsonObject("X").addProperty("item",item(namespace+":crystal_matrix",namespace+":crystal_matrix_block"));
        if(type.equals("avaritia:extreme_crafting_table_jei")){
            var ingredients=new JsonArray();
            for(var line:row.getAsJsonArray("pattern"))for(char c:line.getAsString().toCharArray())
                ingredients.add(c==' '?new JsonArray():key.get(String.valueOf(c)).deepCopy());
            row.add("ingredients",ingredients);row.add("output",row.remove("result"));row.remove("pattern");row.remove("key");
        }
        return row;
    }
    /** Research page material IDs must follow the installed fork too. */
    public static void resolveDisplayItems(JsonElement element){
        if(element.isJsonArray()){for(var child:element.getAsJsonArray())resolveDisplayItems(child);return;}
        if(!element.isJsonObject())return;
        var object=element.getAsJsonObject();
        if(object.has("item")&&object.get("item").isJsonPrimitive()){
            String value=object.get("item").getAsString();
            if(value.startsWith("avaritia:")&&!ForgeRegistries.ITEMS.containsKey(id(value))){
                String alternate="endless:"+value.substring(9);
                if(ForgeRegistries.ITEMS.containsKey(id(alternate)))object.addProperty("item",alternate);
            }
        }
        for(var entry:object.entrySet())resolveDisplayItems(entry.getValue());
    }
    /** Preserve each fork's native catalyst recipe, including datapack changes and feature reloads. */
    public static void applyCatalysts(RecipeManager manager,RegistryAccess registries){
        var pearl=ForgeRegistries.ITEMS.getValue(id("thaumicomniverse:extremely_primordial_pearl"));
        if(pearl==null)return;
        boolean enabled=RevivalFeatures.Group.AVARITIA_CATALYST.enabled();
        for(var recipe:manager.getRecipes()){
            // Re:Avaritia owns a lazy expanded ingredient cache; its dedicated adapter handles it.
            if(recipe instanceof FeatureRecipeMutation)continue;
            String type=Objects.toString(ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer()),"");
            if(!Set.of("avaritia:extreme_shapeless","endless:extreme_craft_shape","avaritia:extreme_crafting_table_jei").contains(type))continue;
            var output=ForgeRegistries.ITEMS.getKey(recipe.getResultItem(registries).getItem());
            if(output==null||!Set.of("avaritia:infinity_catalyst","endless:infinity_catalyst").contains(output.toString()))continue;
            var ingredients=recipe.getIngredients();var previous=ADDED.get(recipe);
            boolean display=type.equals("avaritia:extreme_crafting_table_jei");
            if(!enabled&&previous!=null){
                int index=ingredients.indexOf(previous.added());
                if(index>=0){if(display)ingredients.set(index,previous.replaced());else ingredients.remove(index);}
                ADDED.remove(recipe);
            }
            if(enabled&&ingredients.stream().noneMatch(i->i.test(new ItemStack(pearl)))){
                var added=Ingredient.of(pearl);
                Ingredient replaced=Ingredient.EMPTY;
                if(display){
                    int index=-1;
                    for(int i=0;i<ingredients.size();i++) {
                        var ingredient=ingredients.get(i);var stacks=ingredient.getItems();
                        // Lite represents unused JEI cells as an explicit minecraft:air ingredient.
                        if(ingredient.isEmpty() || stacks.length>0&&Arrays.stream(stacks).allMatch(ItemStack::isEmpty)){index=i;break;}
                    }
                    if(index<0)continue;
                    replaced=ingredients.set(index,added);
                } else ingredients.add(added);
                ADDED.put(recipe,new CatalystAddition(added,replaced));
            }
        }
    }
    @SuppressWarnings("unchecked")
    private static Map<Integer,Slot> liteSlots(Entity entity){
        if(!(entity instanceof Player p)||p.level().isClientSide||
                !p.containerMenu.getClass().getName().equals("net.bullfighter.avaritia.world.inventory.ExtremeCraftingTableGuiMenu"))return null;
        if(p.containerMenu instanceof java.util.function.Supplier<?> supplier&&supplier.get() instanceof Map<?,?> map)
            return (Map<Integer,Slot>)map;
        return null;
    }
    /** Lite's JEI recipes are display only; its Craft button executes procedures instead. */
    public static void craftLiteWand(Entity entity){
        if(!RevivalFeatures.Group.AVARITIA.enabled())return;
        var slots=liteSlots(entity);if(slots==null)return;
        var player=(Player)entity;var optional=player.level().getRecipeManager().byKey(WAND);
        if(optional.isEmpty())return;var recipe=optional.get();var ingredients=recipe.getIngredients();
        if(ingredients.size()!=81)return;
        for(boolean mirror:new boolean[]{false,true}){
            boolean matches=true;
            for(int i=0;i<81;i++){
                var ingredient=ingredients.get((i/9)*9+(mirror?8-i%9:i%9));var stack=slots.get(i).getItem();
                if(ingredient.isEmpty()?!stack.isEmpty():!ingredient.test(stack)){matches=false;break;}
            }
            if(matches){craft(player,slots,recipe.getResultItem(player.level().registryAccess()));return;}
        }
    }
    public static boolean craftLiteCatalyst(Entity entity,List<ItemStack> required,ItemStack output){
        if(!RevivalFeatures.Group.AVARITIA_CATALYST.enabled()||
                !id("avaritia:infinity_catalyst").equals(ForgeRegistries.ITEMS.getKey(output.getItem())))return false;
        var slots=liteSlots(entity);if(slots==null)return true;
        var pearl=ForgeRegistries.ITEMS.getValue(id("thaumicomniverse:extremely_primordial_pearl"));
        if(pearl==null)return true;
        var ingredients=new ArrayList<Ingredient>();for(var stack:required)ingredients.add(Ingredient.of(stack));
        if(ingredients.stream().noneMatch(i->i.test(new ItemStack(pearl))))ingredients.add(Ingredient.of(pearl));
        var input=new ArrayList<ItemStack>();for(int i=0;i<81;i++)if(slots.get(i).hasItem())input.add(slots.get(i).getItem());
        if(net.minecraftforge.common.util.RecipeMatcher.findMatches(input,ingredients)!=null)craft((Player)entity,slots,output);
        return true;
    }
    private static void craft(Player player,Map<Integer,Slot> slots,ItemStack result){
        var existing=slots.get(81).getItem();
        if(!existing.isEmpty()&&(!ItemStack.isSameItemSameTags(existing,result)||existing.getCount()+result.getCount()>existing.getMaxStackSize()))return;
        for(int i=0;i<81;i++){
            var slot=slots.get(i);var input=slot.getItem();if(input.isEmpty())continue;
            var remainder=input.hasCraftingRemainingItem()?input.getCraftingRemainingItem():ItemStack.EMPTY;
            slot.remove(1);
            if(!remainder.isEmpty()){
                if(!slot.hasItem())slot.set(remainder);
                else if(!player.getInventory().add(remainder))player.drop(remainder,false);
            }
        }
        var output=result.copy();output.grow(existing.getCount());slots.get(81).set(output);
        player.containerMenu.broadcastChanges();
    }
}
