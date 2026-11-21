package thmodern.vat;

import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Converts the detached vat subject, never spawning an intermediate world entity. */
public final class CreatureTransformation {
    private CreatureTransformation() {}
    private static EntityType<?> type(JsonObject output) {
        if(!output.has("kind")||!output.get("kind").getAsString().equals("entity")||!output.has("id"))return null;
        var id=ResourceLocation.tryParse(output.get("id").getAsString());
        return id!=null&&ForgeRegistries.ENTITY_TYPES.containsKey(id)?ForgeRegistries.ENTITY_TYPES.getValue(id):null;
    }
    public static boolean canApply(LivingEntity source,JsonObject output) {
        var type=type(output);
        return source instanceof Mob&&source.isAlive()&&type!=null&&type!=source.getType()
            &&type.create(source.level()) instanceof Mob;
    }
    public static Mob prepare(LivingEntity source,VatInfusionRecipe recipe) {
        if(!canApply(source,recipe.output()))return null;
        var result=(Mob)type(recipe.output()).create(source.level());if(result==null)return null;
        var old=new CompoundTag();if(!source.save(old))return null;
        var previous=old.getList("Attributes",Tag.TAG_COMPOUND);
        // AttributeMap creates instances lazily. Materialize supported source attributes before
        // saving destination defaults, or untouched species attributes would lose their modifiers.
        for(var value:previous){
            var id=ResourceLocation.tryParse(((CompoundTag)value).getString("Name"));
            var attribute=id==null?null:ForgeRegistries.ATTRIBUTES.getValue(id);
            if(attribute!=null)result.getAttribute(attribute);
        }
        var defaults=new CompoundTag();result.save(defaults);
        // Keep destination species base stats, and compatible source modifiers exactly once.
        var attributes=defaults.getList("Attributes",Tag.TAG_COMPOUND);
        for(var value:attributes){var attribute=(CompoundTag)value;
            for(var oldValue:previous){var entry=(CompoundTag)oldValue;
                if(entry.getString("Name").equals(attribute.getString("Name"))){
                    if(source instanceof net.minecraft.world.entity.animal.horse.AbstractHorse&&result instanceof net.minecraft.world.entity.animal.horse.AbstractHorse)attribute.putDouble("Base",entry.getDouble("Base"));
                    if(entry.contains("Modifiers",Tag.TAG_LIST))attribute.put("Modifiers",entry.getList("Modifiers",Tag.TAG_COMPOUND).copy());
                }
            }
        }
        old.putString("id",ForgeRegistries.ENTITY_TYPES.getKey(result.getType()).toString());
        old.put("Attributes",attributes);old.remove("Passengers");old.remove("Leash");
        if(source instanceof net.minecraft.world.entity.animal.Ocelot&&result instanceof thmodern.creature.InfusedCat)old.putBoolean("LegacyOcelot",true);
        result.load(old);
        // This is one creature changing species, not a clone: retain UUID, name, gear and owner data.
        var data=result.getPersistentData().getCompound(CreatureInfusions.DATA);
        var costs=data.getCompound("CloneCosts");recipe.aspects.forEach((key,count)->costs.putInt(key,Math.min(32763,Math.max(0,costs.getInt(key))+count)));
        data.put("CloneCosts",costs);result.getPersistentData().put(CreatureInfusions.DATA,data);
        result.setPersistenceRequired();CreatureInfusions.refresh(result);
        result.setHealth(Math.min(source.getHealth(),result.getMaxHealth()));
        return result;
    }
}
