package gadomancymodern.node;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
/** Original disabled-by-default ancient stone recipes remain controlled by the server configuration. */
public class GadConfigCondition implements ICondition {
    public static final ResourceLocation ID=ResourceLocation.parse("gadomancy:ancient_stones");
    @Override public ResourceLocation getID(){return ID;}
    @Override public boolean test(IContext context){return gadomancymodern.aura.AuraEffectsConfig.ANCIENT_STONES.get();}
    public static class Serializer implements IConditionSerializer<GadConfigCondition>{
        @Override public void write(com.google.gson.JsonObject json,GadConfigCondition value){}
        @Override public GadConfigCondition read(com.google.gson.JsonObject json){return new GadConfigCondition();}
        @Override public ResourceLocation getID(){return ID;}
    }
}
