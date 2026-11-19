package gadomancymodern.golem;
import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import tc4revival.content.*;
public class GolemShieldInfusion extends InfusionRecipe {
    protected final JsonObject raw;
    public GolemShieldInfusion(ResourceLocation id,JsonObject json){super(id,json);raw=json.deepCopy();}
    @Override public boolean acceptsCenter(ItemStack center){return center.getItem() instanceof GolemPlacerItem&&!GolemShield.upgraded(center)&&center.getCount()==1;}
    @Override public Plan plan(ItemStack center){var base=super.plan(center);var addons=center.hasTag()?center.getTag().getCompound("AddonData").copy():new net.minecraft.nbt.CompoundTag();var data=addons.getCompound(GadGolems.DATA.toString());data.putBoolean("RunicShield",true);addons.put(GadGolems.DATA.toString(),data);return new Plan(base.components(),base.aspects(),base.instability(),0,false,InfusionOutput.patch("AddonData",addons));}
    @Override public RecipeSerializer<?> getSerializer(){return gadomancymodern.Gadomancy.GOLEM_SHIELD_RECIPE.get();}
    public static class Serializer implements RecipeSerializer<GolemShieldInfusion>{public GolemShieldInfusion fromJson(ResourceLocation id,JsonObject json){return new GolemShieldInfusion(id,json);}public GolemShieldInfusion fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return fromJson(id,JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());}public void toNetwork(FriendlyByteBuf buffer,GolemShieldInfusion recipe){buffer.writeUtf(recipe.raw.toString(),1048576);}}
}
