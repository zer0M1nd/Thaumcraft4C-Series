package thaumicfutures.expansion;

import com.google.gson.*;
import java.util.Arrays;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import it.unimi.dsi.fastutil.ints.*;

/** Use the original plate whenever this pack supplies it; only absent materials fall back. */
public final class PreferredMaterialIngredient extends AbstractIngredient {
    private static final ResourceLocation ID=ResourceLocation.parse("thaumicfutures:preferred_material");
    private static final Codec CODEC=new Codec();
    private final TagKey<Item> preferred;
    private final Ingredient fallback;
    private PreferredMaterialIngredient(ResourceLocation tag,Ingredient fallback){
        preferred=TagKey.create(Registries.ITEM,tag);this.fallback=fallback;
    }
    public static void register(){CraftingHelper.register(ID,CODEC);}
    private boolean available(){return !ForgeRegistries.ITEMS.tags().getTag(preferred).isEmpty();}
    @Override public boolean test(ItemStack stack){return stack!=null&&!stack.isEmpty()&&(available()?stack.is(preferred):fallback.test(stack));}
    @Override public ItemStack[] getItems(){return available()?ForgeRegistries.ITEMS.tags().getTag(preferred).stream().map(ItemStack::new).toArray(ItemStack[]::new):fallback.getItems();}
    @Override public IntList getStackingIds(){
        var ids=new IntArrayList();for(var stack:getItems())ids.add(net.minecraft.world.entity.player.StackedContents.getStackingIndex(stack));
        ids.sort(IntComparators.NATURAL_COMPARATOR);return ids;
    }
    @Override public boolean isEmpty(){return !available()&&fallback.isEmpty();}
    @Override public boolean isSimple(){return false;}
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer(){return CODEC;}
    @Override public JsonElement toJson(){var json=new JsonObject();json.addProperty("type",ID.toString());json.addProperty("tag",preferred.location().toString());json.add("fallback",fallback.toJson());return json;}
    private static final class Codec implements IIngredientSerializer<PreferredMaterialIngredient> {
        @Override public PreferredMaterialIngredient parse(JsonObject json){return new PreferredMaterialIngredient(ResourceLocation.parse(json.get("tag").getAsString()),Ingredient.fromJson(json.get("fallback")));}
        @Override public PreferredMaterialIngredient parse(FriendlyByteBuf buf){return new PreferredMaterialIngredient(buf.readResourceLocation(),Ingredient.fromNetwork(buf));}
        @Override public void write(FriendlyByteBuf buf,PreferredMaterialIngredient value){buf.writeResourceLocation(value.preferred.location());value.fallback.toNetwork(buf);}
    }
}
