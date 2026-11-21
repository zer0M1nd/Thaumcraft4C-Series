package tbmodern;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.*;
import tbmodern.api.RevolverUpgrades;

/** Exact current level plus all upgrade conflicts, evaluated against the live central item. */
public final class RevolverIngredient extends AbstractIngredient {
    public static final ResourceLocation ID = new ResourceLocation("thaumicbases","revolver_upgrade");
    public static final IIngredientSerializer<RevolverIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override public RevolverIngredient parse(JsonObject json) { return new RevolverIngredient(json.get("upgrade").getAsString(),json.get("level").getAsInt()); }
        @Override public RevolverIngredient parse(FriendlyByteBuf buffer) { return new RevolverIngredient(buffer.readUtf(128),buffer.readVarInt()); }
        @Override public void write(FriendlyByteBuf buffer,RevolverIngredient ingredient) { buffer.writeUtf(ingredient.upgrade,128);buffer.writeVarInt(ingredient.level); }
    };
    private final String upgrade;
    private final int level;
    public RevolverIngredient(String upgrade,int level) {
        this.upgrade=upgrade;this.level=level;
        if(level<0 || level>=RevolverUpgrades.require(upgrade).maximum())throw new IllegalArgumentException("Invalid revolver ingredient level");
    }
    @Override public boolean test(ItemStack stack) {
        return stack!=null && stack.getItem() instanceof Revolver && RevolverUpgrades.level(stack,upgrade)==level && RevolverUpgrades.canUpgrade(stack,upgrade);
    }
    @Override public boolean isSimple() {return false;}
    @Override public boolean isEmpty() {return false;}
    // InfusionIngredient checks legacy category equivalence before test(). Returning a representative gun
    // here could allow an externally added ore tag to bypass level/conflict validation.
    @Override public ItemStack[] getItems() {return new ItemStack[0];}
    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer() {return SERIALIZER;}
    @Override public JsonElement toJson() {
        var json=new JsonObject();json.addProperty("type",ID.toString());json.addProperty("upgrade",upgrade);json.addProperty("level",level);return json;
    }
}
