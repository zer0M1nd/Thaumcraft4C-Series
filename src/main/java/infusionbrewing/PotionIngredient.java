package infusionbrewing;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraftforge.common.crafting.*;

/** Potion identity and strength matter; duration and splash form do not. */
public class PotionIngredient extends AbstractIngredient {
    private final int id,amplifier;
    public PotionIngredient(int id,int amplifier){if(!BrewingRules.valid(id)||amplifier<0||amplifier>3)throw new IllegalArgumentException("Invalid potion ingredient");this.id=id;this.amplifier=amplifier;}
    public static final IIngredientSerializer<PotionIngredient> SERIALIZER=new IIngredientSerializer<>(){
        public PotionIngredient parse(FriendlyByteBuf b){return new PotionIngredient(b.readVarInt(),b.readVarInt());}
        public PotionIngredient parse(JsonObject j){return new PotionIngredient(j.get("effect").getAsInt(),j.get("amplifier").getAsInt());}
        public void write(FriendlyByteBuf b,PotionIngredient p){b.writeVarInt(p.id);b.writeVarInt(p.amplifier);}
    };
    @Override public boolean test(ItemStack stack){return stack!=null&&(stack.is(Items.POTION)||stack.is(Items.SPLASH_POTION))&&PotionData.read(stack).filter(p->p.legacyId()==id&&p.amplifier()==amplifier).isPresent();}
    @Override public ItemStack[] getItems(){return new ItemStack[]{new PotionData(id,BrewingRules.instant(id)?1:2400,amplifier).potion(false)};}
    @Override public boolean isSimple(){return false;}
    @Override public boolean isEmpty(){return false;}
    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement toJson(){var j=new JsonObject();j.addProperty("type","infusionbrewing:potion");j.addProperty("effect",id);j.addProperty("amplifier",amplifier);return j;}
}
