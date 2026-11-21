package wgmodern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

/** Original ore dictionary preference: silver when installed, otherwise vanilla iron. */
public class TagOrItemIngredient extends AbstractIngredient {
    public static ResourceLocation ID = new ResourceLocation("witchinggadgets", "tag_or_item");
    public static IIngredientSerializer<TagOrItemIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override public TagOrItemIngredient parse(JsonObject json) {
            return new TagOrItemIngredient(new ResourceLocation(json.get("tag").getAsString()), new ResourceLocation(json.get("fallback").getAsString()));
        }
        @Override public TagOrItemIngredient parse(FriendlyByteBuf buffer) { return new TagOrItemIngredient(buffer.readResourceLocation(), buffer.readResourceLocation()); }
        @Override public void write(FriendlyByteBuf buffer, TagOrItemIngredient value) { buffer.writeResourceLocation(value.tag.location()); buffer.writeResourceLocation(value.fallback); }
    };
    private TagKey<Item> tag;
    private ResourceLocation fallback;
    public TagOrItemIngredient(ResourceLocation tag, ResourceLocation fallback) { this.tag = TagKey.create(Registries.ITEM, tag); this.fallback = fallback; }
    @Override public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var values = ForgeRegistries.ITEMS.tags().getTag(tag);
        return values.isEmpty() ? stack.is(ForgeRegistries.ITEMS.getValue(fallback)) : stack.is(tag);
    }
    @Override public boolean isSimple() { return false; }
    @Override public boolean isEmpty() { return false; }
    @Override public ItemStack[] getItems() {
        var values = ForgeRegistries.ITEMS.tags().getTag(tag);
        return values.isEmpty() ? new ItemStack[]{new ItemStack(ForgeRegistries.ITEMS.getValue(fallback))} : values.stream().map(ItemStack::new).toArray(ItemStack[]::new);
    }
    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer() { return SERIALIZER; }
    @Override public JsonElement toJson() {
        var result = new JsonObject(); result.addProperty("type", ID.toString()); result.addProperty("tag", tag.location().toString()); result.addProperty("fallback", fallback.toString()); return result;
    }
}
