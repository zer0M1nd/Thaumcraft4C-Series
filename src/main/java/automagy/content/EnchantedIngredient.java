package automagy.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Objects;
import java.util.stream.Stream;

/** Minimum-level enchantment ingredient, including stored book enchantments and worn tools. */
public final class EnchantedIngredient extends AbstractIngredient {
    public static final Serializer SERIALIZER = new Serializer();
    private final Item item;
    private final Enchantment enchantment;
    private final int minimum;
    public EnchantedIngredient(Item item, Enchantment enchantment, int minimum) {
        super(Stream.of(new ItemValue(example(item, enchantment, minimum))));
        this.item=item; this.enchantment=enchantment; this.minimum=minimum;
        if(minimum<1 || minimum>255) throw new IllegalArgumentException("Enchantment level must be 1..255");
    }
    private static ItemStack example(Item item, Enchantment enchantment, int minimum) {
        ItemStack stack=new ItemStack(item);
        if(item==Items.ENCHANTED_BOOK) EnchantedBookItem.addEnchantment(stack,new EnchantmentInstance(enchantment,minimum));
        else stack.enchant(enchantment,minimum);
        return stack;
    }
    @Override public boolean test(ItemStack stack) {
        return stack!=null && stack.is(item) && EnchantmentHelper.getEnchantments(stack).getOrDefault(enchantment,0)>=minimum;
    }
    @Override public boolean isSimple(){return false;}
    @Override public IIngredientSerializer<EnchantedIngredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement toJson(){
        JsonObject json=new JsonObject(); json.addProperty("type","automagy:enchanted");
        json.addProperty("item",ForgeRegistries.ITEMS.getKey(item).toString());
        json.addProperty("enchantment",ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString());
        json.addProperty("minimum",minimum); return json;
    }
    public static final class Serializer implements IIngredientSerializer<EnchantedIngredient> {
        @Override public EnchantedIngredient parse(JsonObject json){
            return new EnchantedIngredient(CraftingHelper.getItem(json.get("item").getAsString(),true),
                Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(json.get("enchantment").getAsString())),"Unknown enchantment"),json.get("minimum").getAsInt());
        }
        @Override public EnchantedIngredient parse(FriendlyByteBuf buffer){return parse(com.google.gson.JsonParser.parseString(buffer.readUtf()).getAsJsonObject());}
        @Override public void write(FriendlyByteBuf buffer,EnchantedIngredient ingredient){buffer.writeUtf(ingredient.toJson().toString());}
    }
}
