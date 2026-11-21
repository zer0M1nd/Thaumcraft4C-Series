package gadomancymodern.disguise;
import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraftforge.common.crafting.*;

/** Dynamic modded armor support; matching does not depend on a manually maintained armor tag. */
public class DisguiseIngredient extends AbstractIngredient {
    protected ItemStack[] examples;
    public static final IIngredientSerializer<DisguiseIngredient> SERIALIZER=new IIngredientSerializer<>(){
        public DisguiseIngredient parse(FriendlyByteBuf buffer){return new DisguiseIngredient();}
        public DisguiseIngredient parse(JsonObject json){return new DisguiseIngredient();}
        public void write(FriendlyByteBuf buffer,DisguiseIngredient ingredient){}
    };
    public static void register(){CraftingHelper.register(net.minecraft.resources.ResourceLocation.parse("gadomancy:disguise_material"),SERIALIZER);}
    @Override public boolean test(ItemStack stack){return stack!=null&&!ArmorDisguise.disguised(stack)&&(ArmorDisguise.armor(stack)||ArmorDisguise.invisible(stack));}
    @Override public ItemStack[] getItems(){
        if(examples==null){var items=new java.util.ArrayList<ItemStack>();net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().forEach(item->{var stack=item.getDefaultInstance();if(ArmorDisguise.armor(stack))items.add(stack);});for(var item:java.util.List.of(Items.POTION,Items.SPLASH_POTION))for(var potion:java.util.List.of(net.minecraft.world.item.alchemy.Potions.INVISIBILITY,net.minecraft.world.item.alchemy.Potions.LONG_INVISIBILITY))items.add(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(item),potion));examples=items.toArray(ItemStack[]::new);}return examples;
    }
    @Override public boolean isSimple(){return false;}
    @Override public boolean isEmpty(){return false;}
    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement toJson(){var json=new JsonObject();json.addProperty("type","gadomancy:disguise_material");return json;}
}
