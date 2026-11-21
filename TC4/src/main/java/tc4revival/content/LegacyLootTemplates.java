package tc4revival.content;

import com.google.gson.JsonObject;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Loot templates are immutable; random charge is rolled for each selected reward. */
public  class LegacyLootTemplates {
    private LegacyLootTemplates() {}
    public static ItemStack prechargedAmulet() {
        return prechargedAmulet(net.minecraft.util.RandomSource.create()::nextInt);
    }
    public static ItemStack prechargedAmulet(java.util.function.IntUnaryOperator random) {
        var stack=new ItemStack(TCItems.LESSER_VIS_AMULET.get());
        var item=(VisAmuletItem)stack.getItem();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())item.addVis(stack,aspect,random.applyAsInt(5),true,true);
        return stack;
    }
    public static ItemStack decode(JsonObject definition) {
        if (GsonHelper.getAsBoolean(definition,"precharged_amulet",false)) return prechargedAmulet();
        var stack=ShapedRecipe.itemStackFromJson(definition);
        if (definition.has("nbt")) try { stack.setTag(TagParser.parseTag(GsonHelper.getAsString(definition,"nbt"))); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalArgumentException("Invalid legacy loot NBT",failure); }
        return stack;
    }
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {}
}
