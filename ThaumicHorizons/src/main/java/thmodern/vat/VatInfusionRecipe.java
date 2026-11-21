package thmodern.vat;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

/** Addon-owned datapack recipes; no base recipe serializer or enum needs changing. */
public final class VatInfusionRecipe {
    public final ResourceLocation id;
    public final String research,targetKind;
    public final int instability;
    public final List<Ingredient> ingredients;
    public final Map<String,Integer> aspects;
    private final JsonObject definition;
    public VatInfusionRecipe(ResourceLocation id,JsonObject json){
        this.id=id;definition=json.deepCopy();research=json.get("research").getAsString();targetKind=json.get("target").getAsJsonObject().get("kind").getAsString();
        if(!Set.of("living","entity","tag","self").contains(targetKind))throw new JsonParseException("Unknown vat target: "+targetKind);
        instability=json.get("instability").getAsInt();if(instability<0||instability>25)throw new JsonParseException("Instability must be 0..25");
        var components=new ArrayList<Ingredient>();for(var value:json.getAsJsonArray("ingredients")){var ingredient=Ingredient.fromJson(value);if(ingredient.isEmpty())throw new JsonParseException("Empty vat ingredient");components.add(ingredient);}
        if(components.isEmpty()||components.size()>64)throw new JsonParseException("Vat recipes require 1..64 ingredients");ingredients=List.copyOf(components);
        var costs=new LinkedHashMap<String,Integer>();for(var entry:json.getAsJsonObject("aspects").entrySet())costs.put(entry.getKey(),entry.getValue().getAsInt());
        var validated=new VatEssentia();validated.demand(costs);if(costs.isEmpty())throw new JsonParseException("Missing infusion essentia");aspects=Collections.unmodifiableMap(costs);
        if(!json.has("output")||!json.get("output").isJsonObject())throw new JsonParseException("Missing infusion output");
    }
    public JsonObject json(){return definition.deepCopy();}
    public JsonObject output(){return definition.getAsJsonObject("output").deepCopy();}
    public boolean target(LivingEntity entity,boolean effigy){
        if(targetKind.equals("self"))return effigy;
        if(effigy||entity==null||entity instanceof net.minecraft.world.entity.player.Player||entity instanceof net.minecraft.world.entity.animal.AbstractGolem||entity instanceof net.minecraft.world.item.trading.Merchant||entity instanceof net.minecraft.world.entity.npc.Npc||entity instanceof tc4revival.content.TaintedMob||entity.getMobType()==MobType.UNDEAD||entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("thaumichorizons:infusion_blacklist"))))return false;
        var target=definition.getAsJsonObject("target");
        return switch(targetKind){
            case "living"->true;
            case "entity"->entity.getType()==net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(target.get("id").getAsString()));
            case "tag"->entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse(target.get("id").getAsString())));
            default->false;
        };
    }
    public Optional<int[]> match(ServerPlayer player,LivingEntity entity,boolean effigy,List<ItemStack> stacks){
        if(!target(entity,effigy)||!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,research))return Optional.empty();
        return IngredientAssignment.match(ingredients,stacks);
    }
}
