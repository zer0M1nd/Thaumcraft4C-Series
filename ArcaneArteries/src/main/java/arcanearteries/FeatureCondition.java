package arcanearteries;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import arcanearteries.SacrificeValues.School;

public record FeatureCondition(String feature, boolean hard) implements ICondition {
    @Override public ResourceLocation getID() { return ArcaneArteries.id("feature"); }
    @Override public boolean test(IContext context) {
        return switch (feature) {
            case "slates" -> ArteriesConfig.value(ArteriesConfig.SLATES);
            case "upgrade" -> ArteriesConfig.value(ArteriesConfig.UPGRADE);
            case "thaumic_self" -> ArcaneArteries.available(School.THAUMIC, false) && ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING) == hard;
            case "thaumic_mob" -> ArcaneArteries.available(School.THAUMIC, true) && ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING) == hard;
            case "botanical_self" -> ArcaneArteries.available(School.BOTANICAL, false) && ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING) == hard;
            case "botanical_mob" -> ArcaneArteries.available(School.BOTANICAL, true) && ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING) == hard;
            case "evil_self" -> ArcaneArteries.available(School.EVIL, false);
            case "evil_mob" -> ArcaneArteries.available(School.EVIL, true);
            default -> throw new IllegalArgumentException("Unknown Arcane Arteries feature: " + feature);
        };
    }
    public static final class Serializer implements IConditionSerializer<FeatureCondition> {
        @Override public void write(JsonObject json, FeatureCondition value) { json.addProperty("feature", value.feature()); json.addProperty("hard", value.hard()); }
        @Override public FeatureCondition read(JsonObject json) { return new FeatureCondition(GsonHelper.getAsString(json, "feature"), GsonHelper.getAsBoolean(json, "hard", false)); }
        @Override public ResourceLocation getID() { return ArcaneArteries.id("feature"); }
    }
}
