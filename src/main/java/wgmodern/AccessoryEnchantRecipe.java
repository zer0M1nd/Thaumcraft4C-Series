package wgmodern;
import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.InfusionRecipe;

/** Original WG isItemTool equipment can be infused despite having no durability. */
public class AccessoryEnchantRecipe extends InfusionRecipe {
    private final Enchantment enchantment;
    public AccessoryEnchantRecipe(ResourceLocation id,JsonObject json){super(id,json);enchantment=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(json.get("enchantment").getAsString()));}
    @Override public boolean acceptsCenter(ItemStack center){return super.acceptsCenter(center);}

    @Override public RecipeSerializer<?> getSerializer(){return Content.GEAR_ENCHANTMENT.get();}
    public static class Serializer implements RecipeSerializer<AccessoryEnchantRecipe>{public AccessoryEnchantRecipe fromJson(ResourceLocation id,JsonObject json){return new AccessoryEnchantRecipe(id,json);}public AccessoryEnchantRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf(1048576)).getAsJsonObject());}public void toNetwork(FriendlyByteBuf b,AccessoryEnchantRecipe r){b.writeUtf(r.displayDefinition().toString(),1048576);}}
}
