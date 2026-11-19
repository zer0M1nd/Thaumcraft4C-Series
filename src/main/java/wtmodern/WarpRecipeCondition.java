package wtmodern;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
public class WarpRecipeCondition implements ICondition {
    private final boolean easy;
    public WarpRecipeCondition(boolean easy){this.easy=easy;}
    @Override public ResourceLocation getID(){return new ResourceLocation("warptheory","easy_recipes");}
    @Override public boolean test(IContext context){return WarpConfig.EASY.get()==easy;}
    public static class Serializer implements IConditionSerializer<WarpRecipeCondition>{
        public void write(JsonObject json,WarpRecipeCondition condition){json.addProperty("easy",condition.easy);}
        public WarpRecipeCondition read(JsonObject json){return new WarpRecipeCondition(json.get("easy").getAsBoolean());}
        public ResourceLocation getID(){return new ResourceLocation("warptheory","easy_recipes");}
    }
}
