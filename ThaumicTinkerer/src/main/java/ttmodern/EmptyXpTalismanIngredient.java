package ttmodern;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.*;

/** An upgrade must not destroy blocks stored inside the ingredient. */
public final class EmptyXpTalismanIngredient extends AbstractIngredient {
    public static final IIngredientSerializer<EmptyXpTalismanIngredient> SERIALIZER=new IIngredientSerializer<>(){
        public EmptyXpTalismanIngredient parse(FriendlyByteBuf buffer){return new EmptyXpTalismanIngredient();}
        public EmptyXpTalismanIngredient parse(JsonObject json){return new EmptyXpTalismanIngredient();}
        public void write(FriendlyByteBuf buffer,EmptyXpTalismanIngredient ingredient){}
    };
    public static void register(){CraftingHelper.register(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":empty_xp_talisman"),SERIALIZER);}
    @Override public boolean test(ItemStack stack){return stack!=null&&stack.is(Tinkerer.XP_TALISMAN.get())&&XpTalismanItem.xp(stack)==0;}
    @Override public ItemStack[] getItems(){return new ItemStack[]{new ItemStack(Tinkerer.XP_TALISMAN.get())};}
    @Override public boolean isSimple(){return false;}
    @Override public boolean isEmpty(){return false;}
    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement toJson(){var json=new JsonObject();json.addProperty("type",Tinkerer.ID+":empty_xp_talisman");return json;}
}
