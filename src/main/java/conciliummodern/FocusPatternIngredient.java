package conciliummodern;

import com.google.gson.*;
import java.util.List;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.common.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

/** An ordered five-rank focus recipe, independent of runtime upgrade numeric IDs. */
public final class FocusPatternIngredient extends AbstractIngredient {
    public static final ResourceLocation ID=ResourceLocation.parse("thaumicconcilium:focus_pattern");
    public static final IIngredientSerializer<FocusPatternIngredient> SERIALIZER=new IIngredientSerializer<>() {
        @Override public FocusPatternIngredient parse(JsonObject json){
            var ranks=new java.util.ArrayList<String>();
            for(var entry:json.getAsJsonArray("upgrades"))ranks.add(entry.getAsString());
            return new FocusPatternIngredient(ResourceLocation.parse(json.get("item").getAsString()),ranks);
        }
        @Override public FocusPatternIngredient parse(FriendlyByteBuf buffer){
            var item=buffer.readResourceLocation();var ranks=new java.util.ArrayList<String>();
            for(int i=0;i<5;i++)ranks.add(buffer.readUtf(256));return new FocusPatternIngredient(item,ranks);
        }
        @Override public void write(FriendlyByteBuf buffer,FocusPatternIngredient value){
            buffer.writeResourceLocation(value.item);for(var rank:value.ranks)buffer.writeUtf(rank,256);
        }
    };
    private final ResourceLocation item;
    private final List<String> ranks;
    public FocusPatternIngredient(ResourceLocation item,List<String> ranks){
        if(ranks.size()!=5)throw new IllegalArgumentException("A focus pattern needs exactly five ranks");
        if(!(ForgeRegistries.ITEMS.getValue(item) instanceof WandFocusItem))throw new IllegalArgumentException("Not a focus: "+item);
        for(var rank:ranks)FocusUpgrade.require(rank);
        this.item=item;this.ranks=List.copyOf(ranks);
    }
    @Override public boolean test(ItemStack stack){
        if(stack==null || stack.isEmpty() || !stack.is(ForgeRegistries.ITEMS.getValue(item)) || !stack.hasTag())return false;
        var entries=stack.getTag().getList("upgrade",Tag.TAG_COMPOUND);if(entries.size()!=5)return false;
        for(int i=0;i<5;i++)if(FocusUpgrade.savedId(entries.getCompound(i))!=FocusUpgrade.require(ranks.get(i)).id)return false;
        return true;
    }
    @Override public ItemStack[] getItems(){
        var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(item));var entries=new ListTag();
        for(var key:ranks){var tag=new CompoundTag();tag.putString("key",key);tag.putShort("id",(short)FocusUpgrade.require(key).id);entries.add(tag);}
        stack.getOrCreateTag().put("upgrade",entries);return new ItemStack[]{stack};
    }
    @Override public boolean isSimple(){return false;}
    @Override public boolean isEmpty(){return false;}
    @Override public IIngredientSerializer<FocusPatternIngredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement toJson(){
        var json=new JsonObject();json.addProperty("type",ID.toString());json.addProperty("item",item.toString());
        var list=new JsonArray();ranks.forEach(list::add);json.add("upgrades",list);return json;
    }
}
