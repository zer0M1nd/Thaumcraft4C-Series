package tc4revival.content;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** One occupied pedestal, including the old top-level crafting-tag comparison. */
public  class InfusionIngredient {
    private final JsonObject definition;
    private final Ingredient ingredient;
    private final CompoundTag requiredTag;
    private final int damage;
    private final boolean exact;
    public InfusionIngredient(JsonObject json) {
        definition=json.deepCopy(); ingredient=Ingredient.fromJson(json.get("ingredient"));
        damage=GsonHelper.getAsInt(json,"damage",0);
        exact=GsonHelper.getAsBoolean(json,"exact",false);
        if (damage < -1) throw new IllegalArgumentException("Invalid infusion ingredient damage");
        try { requiredTag=json.has("nbt") ? TagParser.parseTag(GsonHelper.getAsString(json,"nbt")) : null; }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalArgumentException("Invalid infusion ingredient NBT",failure); }
    }
    public JsonObject definition() { return definition.deepCopy(); }
    public java.util.List<ItemStack> displayStacks() {
        // Custom ingredients may intentionally expose no getItems() representatives to
        // prevent legacy material equivalence from bypassing their NBT checks.
        // Explicit display samples never participate in matches().
        if(definition.has("display")) {
            var result=new java.util.ArrayList<ItemStack>();
            for(var element:definition.getAsJsonArray("display")) {
                var json=element.getAsJsonObject();
                var stack=net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(json);
                if(json.has("nbt"))try { stack.setTag(TagParser.parseTag(GsonHelper.getAsString(json,"nbt"))); }
                catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure) {throw new IllegalArgumentException("Invalid infusion display NBT",failure);}
                if(matches(stack))result.add(stack);
            }
            return java.util.List.copyOf(result);
        }
        return java.util.Arrays.stream(ingredient.getItems()).map(stack->{var copy=stack.copy();
            if(requiredTag!=null)copy.setTag(requiredTag.copy());
            if(damage>0&&damage!=32767&&copy.isDamageableItem())copy.setDamageValue(damage);
            return copy;}).toList();
    }
    public boolean matches(ItemStack offered) {
        if (offered.isEmpty() || !craftingTagsMatch(offered.getTag(),requiredTag)) return false;
        if(!exact)for (var expected : ingredient.getItems()) if (LegacyCraftingCategories.matches(offered,expected)) return true;
        return offered.getCount()<=offered.getMaxStackSize() && ingredient.test(offered)
                && (damage<0 || damage==32767 || metadata(offered)==damage);
    }
    /** Ongoing infusion preserves the chosen center item and its required NBT, ignoring only tool wear. */
    public static boolean centerMatches(ItemStack actual, ItemStack expected) {
        if (actual.isEmpty() || expected.isEmpty()) return false;
        if (actual.getItem()!=expected.getItem() || actual.getCount()>actual.getMaxStackSize()) return false;
        boolean ignoreWear=actual.isDamageableItem() && expected.isDamageableItem();
        if (!ignoreWear && metadata(actual)!=metadata(expected)) return false;
        var required=expected.hasTag() ? expected.getTag().copy() : null;
        var offered=actual.hasTag() ? actual.getTag().copy() : null;
        if (ignoreWear) {
            if (required!=null) required.remove("Damage");
            if (offered!=null) offered.remove("Damage");
            if (required!=null && required.isEmpty()) required=null;
        }
        return craftingTagsMatch(offered,required);
    }
    public static boolean craftingTagsMatch(CompoundTag offered,CompoundTag required) {
        if (required==null) return true;
        if (offered==null) return false;
        // Extra top-level tags are allowed; each required nested value must match completely.
        for (String key:required.getAllKeys()) if (!required.get(key).equals(offered.get(key))) return false;
        return true;
    }
    private static int metadata(ItemStack stack) { return stack.getItem() instanceof ItemMirrorItem ? ItemMirrorItem.legacyMetadata(stack) : stack.getDamageValue(); }
}
