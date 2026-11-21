package tc4revival.research;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
import net.minecraftforge.fml.config.ModConfig;

/** Shared by complete pages and TEXT_CONCEALED through Forge's condition tree. */
public record BookConfigCondition(String mod, String scope, String key, String operator, JsonElement expected) implements ICondition {
    public static final ResourceLocation ID=ResourceLocation.parse("thaumcraft:config");
    @Override public ResourceLocation getID(){return ID;}
    @Override public boolean test(IContext context) {
        var type=ModConfig.Type.valueOf(scope.toUpperCase(java.util.Locale.ROOT));
        for(var config:net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(type)) {
            if(!config.getModId().equals(mod)||config.getConfigData()==null)continue;
            Object value=config.getConfigData().get(key);
            if(value!=null)return compare(new Gson().toJsonTree(value),operator,expected);
        }
        return false;
    }
    public static boolean compare(JsonElement actual,String operator,JsonElement expected) {
        return switch(operator) {
            case "eq","==" -> actual.equals(expected);
            case "ne","!=" -> !actual.equals(expected);
            case "lt","<","lte","<=","gt",">","gte",">=" -> {
                if(!actual.isJsonPrimitive()||!actual.getAsJsonPrimitive().isNumber()
                        ||!expected.isJsonPrimitive()||!expected.getAsJsonPrimitive().isNumber())yield false;
                int order=actual.getAsBigDecimal().compareTo(expected.getAsBigDecimal());
                yield switch(operator){case "lt","<"->order<0;case "lte","<="->order<=0;
                    case "gt",">"->order>0;default->order>=0;};
            }
            case "in" -> expected.isJsonArray()&&expected.getAsJsonArray().contains(actual);
            case "contains" -> actual.isJsonArray()?actual.getAsJsonArray().contains(expected):
                    actual.isJsonPrimitive()&&actual.getAsJsonPrimitive().isString()&&expected.isJsonPrimitive()
                            &&actual.getAsString().contains(expected.getAsString());
            default -> throw new IllegalArgumentException("Unknown config comparison "+operator);
        };
    }
    public static class Serializer implements IConditionSerializer<BookConfigCondition> {
        @Override public ResourceLocation getID(){return ID;}
        @Override public BookConfigCondition read(JsonObject json) {
            return new BookConfigCondition(json.get("modid").getAsString(),json.has("scope")?json.get("scope").getAsString():"server",
                    json.get("key").getAsString(),json.has("operator")?json.get("operator").getAsString():"eq",json.get("value").deepCopy());
        }
        @Override public void write(JsonObject json,BookConfigCondition value) {
            json.addProperty("modid",value.mod());json.addProperty("scope",value.scope());json.addProperty("key",value.key());
            json.addProperty("operator",value.operator());json.add("value",value.expected().deepCopy());
        }
    }
}
