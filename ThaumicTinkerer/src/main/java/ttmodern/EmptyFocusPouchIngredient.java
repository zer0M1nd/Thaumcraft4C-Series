package ttmodern;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.*;

/** An upgrade must never consume the foci stored inside its input bag. */
public final class EmptyFocusPouchIngredient extends AbstractIngredient {
    public static final IIngredientSerializer<EmptyFocusPouchIngredient> SERIALIZER=new IIngredientSerializer<>(){
        public EmptyFocusPouchIngredient parse(FriendlyByteBuf buffer){return new EmptyFocusPouchIngredient();}
        public EmptyFocusPouchIngredient parse(JsonObject json){return new EmptyFocusPouchIngredient();}
        public void write(FriendlyByteBuf buffer,EmptyFocusPouchIngredient ingredient){}
    };
    public static void register(){CraftingHelper.register(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":empty_focus_pouch"),SERIALIZER);}
    @Override public boolean test(ItemStack stack){return stack!=null&&stack.getItem() instanceof tc4revival.content.FocusPouchItem&&(!stack.hasTag()||stack.getTag().getList("Inventory",net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty());}
    @Override public ItemStack[] getItems(){return new ItemStack[]{new ItemStack(tc4revival.content.TCItems.FOCUS_POUCH.get())};}
    @Override public boolean isSimple(){return false;}
    @Override public boolean isEmpty(){return false;}
    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement toJson(){var json=new JsonObject();json.addProperty("type",Tinkerer.ID+":empty_focus_pouch");return json;}
}
