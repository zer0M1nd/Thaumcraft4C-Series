package advancedthaumaturgy;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import tc4revival.content.*;
public class WandUpgradeRecipe extends InfusionRecipe {
    private final CrystalItem.Upgrade upgrade;
    public WandUpgradeRecipe(ResourceLocation id,JsonObject data){super(id,data);upgrade=CrystalItem.Upgrade.valueOf(data.get("upgrade").getAsString().toUpperCase(java.util.Locale.ROOT));}
    @Override public boolean acceptsCenter(ItemStack stack){return ATConfig.enabled("wand_upgrades")&&stack.getItem() instanceof MercurialWandItem&&!CrystalItem.has(stack,upgrade)&&super.acceptsCenter(stack);}
    @Override public Plan plan(ItemStack center){var base=super.plan(center);var result=center.copyWithCount(1);CrystalItem.apply(result,upgrade);return new Plan(base.components(),base.aspects(),base.instability(),base.xp(),base.enchanting(),InfusionOutput.item(result));}
    @Override public ItemStack assemble(InfusionRecipe.Input input,net.minecraft.core.RegistryAccess registries){return plan(input.getItem(0)).output().apply(input.getItem(0));}
    @Override public ItemStack getResultItem(net.minecraft.core.RegistryAccess registries){var result=AdvancedThaumaturgy.MERCURIAL_WAND.get().create(WandParts.Rod.require(MercurialWandItem.ROD),WandParts.Cap.THAUMIUM,false,false);CrystalItem.apply(result,upgrade);return result;}
    @Override public RecipeSerializer<?> getSerializer(){return AdvancedThaumaturgy.WAND_UPGRADE.get();}
    public static class Serializer implements RecipeSerializer<WandUpgradeRecipe>{
        public WandUpgradeRecipe fromJson(ResourceLocation id,JsonObject data){return new WandUpgradeRecipe(id,data);}
        public WandUpgradeRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,com.google.gson.JsonParser.parseString(b.readUtf()).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf b,WandUpgradeRecipe recipe){b.writeUtf(recipe.displayDefinition().toString());}
    }
}
