package fmmodern;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/** Evaluated on server data load/reload, after world configuration is available. */
public record RecipeConfigCondition(String option,boolean expected) implements ICondition {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"config");
    @Override public ResourceLocation getID(){return ID;}
    @Override public boolean test(IContext context){return value(option)==expected;}
    public static boolean value(String option){return switch(option){
        case "lust" -> !fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST);
        case "cage" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.WRATH_CAGE);
        case "enchantments" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.ENCHANTING);
        case "greedy" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.ENCHANTING)&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.GREEDY);
        case "emerald" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.EMERALD_TRANS);
        case "food_enabled" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.GLUTTONY)!=1;
        case "food_hard" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.GLUTTONY)==2;
        case "easy_research" -> tc4revival.config.TCConfig.SERVER_SPEC.isLoaded()&&tc4revival.config.TCConfig.SERVER.researchDifficulty.get()==-1;
        case "botania" -> IntegrationContent.botanyEnabled();
        case "bloodmagic" -> IntegrationContent.bloodEnabled();
        case "conversion_wands" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD)&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND);
        default -> throw new IllegalArgumentException("Unknown Forbidden Magic recipe option "+option);
    };}
    public static final class Serializer implements IConditionSerializer<RecipeConfigCondition> {
        @Override public ResourceLocation getID(){return ID;}
        @Override public RecipeConfigCondition read(JsonObject json){return new RecipeConfigCondition(json.get("option").getAsString(),!json.has("expected")||json.get("expected").getAsBoolean());}
        @Override public void write(JsonObject json,RecipeConfigCondition value){json.addProperty("option",value.option());json.addProperty("expected",value.expected());}
    }
}
