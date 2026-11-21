package gadomancymodern.familiar;

import com.google.gson.*;
import gadomancymodern.item.EtherealFamiliarItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

/** An aspect-bound immutable recipe; matching never stores another altar's last selected aspect. */
public class FamiliarInfusion extends InfusionRecipe {
    protected final JsonObject source;
    public final String aspect,augment;
    public final int requiredPrevious;
    public FamiliarInfusion(ResourceLocation id,JsonObject json){super(id,expand(json));source=json.deepCopy();aspect=json.get("familiar_aspect").getAsString();augment=json.has("augment")?json.get("augment").getAsString():"";requiredPrevious=json.has("previous_level")?json.get("previous_level").getAsInt():0;}
    public static JsonObject expand(JsonObject source){
        var json=source.deepCopy();String aspect=json.get("familiar_aspect").getAsString();if(aspect.equals("*"))aspect="praecantatio";
        var costs=json.getAsJsonObject("aspects");int extra=json.get("aspect_cost").getAsInt();costs.addProperty(aspect,extra+(costs.has(aspect)?costs.get(aspect).getAsInt():0));
        for(var element:json.getAsJsonArray("components")){var ingredient=element.getAsJsonObject();if(ingredient.getAsJsonObject("ingredient").has("item")&&ingredient.getAsJsonObject("ingredient").get("item").getAsString().equals("thaumcraft:wisp_essence"))ingredient.addProperty("nbt","{Aspects:[{key:\""+aspect+"\",amount:2}]}");}
        json.getAsJsonObject("result").addProperty("nbt","{aspect:\""+aspect+"\"}");return json;
    }
    public boolean template(){return aspect.equals("*");}
    @Override public boolean matches(Input input,Level level){
        String research=source.has("research")?source.get("research").getAsString():"";
        if(!thaumcraft.api.research.ResearchCategories.enabled(research)||!input.researchComplete(research)||!acceptsCenter(input.getItem(0)))return false;
        var available=new java.util.ArrayList<ItemStack>();for(int slot=1;slot<input.getContainerSize();slot++)if(!input.getItem(slot).isEmpty())available.add(input.getItem(slot));
        for(var ingredient:super.plan(input.getItem(0)).components()){int found=-1;for(int i=0;i<available.size();i++)if(ingredient.matches(available.get(i))){found=i;break;}if(found<0)return false;available.remove(found);}
        return true; // Legacy familiar recipes accept and leave unrelated pedestal items untouched.
    }
    @Override public boolean acceptsCenter(ItemStack center){
        if(template()||!super.acceptsCenter(center))return false;
        if(augment.isEmpty())return true;
        if(!EtherealFamiliarItem.aspect(center).equals(aspect))return false;var definition=FamiliarAugment.REGISTRY.get(augment);var current=EtherealFamiliarItem.augments(center);int level=current.getOrDefault(augment,0);
        return definition!=null&&level>=requiredPrevious&&definition.accepts(current,level+1);
    }
    @Override public Plan plan(ItemStack center){var original=super.plan(center);if(augment.isEmpty())return original;var output=center.copyWithCount(1);EtherealFamiliarItem.augment(output,augment,EtherealFamiliarItem.augments(center).getOrDefault(augment,0)+1);return new Plan(original.components(),original.aspects(),original.instability(),0,false,InfusionOutput.item(output));}
    @Override public ItemStack assemble(Input input,RegistryAccess registries){return plan(input.getItem(0)).output().apply(input.getItem(0));}
    public JsonObject templateDefinition(){return source.deepCopy();}
    @Override public ItemStack getResultItem(RegistryAccess registries){var result=super.getResultItem(registries);if(!augment.isEmpty())EtherealFamiliarItem.augment(result,augment,requiredPrevious+1);return result;}
    @Override public JsonObject displayDefinition(){
        var json=super.displayDefinition();
        if(!augment.isEmpty()&&!template()){
            var center=super.getResultItem(RegistryAccess.EMPTY).copyWithCount(1);
            EtherealFamiliarItem.aspect(center,aspect);
            if(requiredPrevious>0)EtherealFamiliarItem.augment(center,augment,requiredPrevious);
            var definition=FamiliarAugment.REGISTRY.get(augment);
            if(definition!=null&&!definition.element)EtherealFamiliarItem.augment(center,"shock",1);
            json.getAsJsonObject("center").addProperty("nbt",center.getTag().toString());
            json.getAsJsonObject("result").addProperty("nbt",plan(center).output().apply(center).getTag().toString());
        }
        return json;
    }
    @Override public RecipeSerializer<?> getSerializer(){return FamiliarRecipes.SERIALIZER.get();}
    public static class Serializer implements RecipeSerializer<FamiliarInfusion> {
        @Override public FamiliarInfusion fromJson(ResourceLocation id,JsonObject json){return new FamiliarInfusion(id,json);}
        @Override public FamiliarInfusion fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return fromJson(id,JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());}
        @Override public void toNetwork(FriendlyByteBuf buffer,FamiliarInfusion recipe){buffer.writeUtf(recipe.source.toString(),1048576);}
    }
}
