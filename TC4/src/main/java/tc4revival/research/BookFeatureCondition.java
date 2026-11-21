package tc4revival.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.RevivalConfig;

/** Effective tri-state feature value, including profile and dependent DEFAULT rules. */
public record BookFeatureCondition(String mod, String scope, String key, boolean expected) implements ICondition {
    public static final ResourceLocation ID = ResourceLocation.parse("thaumcraft:feature");
    @Override public ResourceLocation getID() { return ID; }
    @Override public boolean test(IContext context) {
        var type = ModConfig.Type.valueOf(scope.toUpperCase(java.util.Locale.ROOT));
        for (var config : net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(type)) {
            if (!config.getModId().equals(mod)) continue;
            var features = RevivalConfig.forSpec(config.getSpec());
            if (features == null) continue;
            String path = key.startsWith("revival_features.") ? key.substring("revival_features.".length()) : key;
            var option = features.options().get(path);
            if (option != null) return option.getAsBoolean() == expected;
        }
        return false;
    }
    public static class Serializer implements IConditionSerializer<BookFeatureCondition> {
        @Override public ResourceLocation getID() { return ID; }
        @Override public BookFeatureCondition read(JsonObject json) {
            return new BookFeatureCondition(json.get("modid").getAsString(),
                    json.has("scope") ? json.get("scope").getAsString() : "server", json.get("key").getAsString(),
                    !json.has("value") || json.get("value").getAsBoolean());
        }
        @Override public void write(JsonObject json, BookFeatureCondition value) {
            json.addProperty("modid", value.mod()); json.addProperty("scope", value.scope());
            json.addProperty("key", value.key()); json.addProperty("value", value.expected());
        }
    }
}
