package arcane_engineering;

import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.content.*;
import java.util.*;
import java.util.function.Predicate;

public class UpgradeableWandRecipe extends ArcaneRecipe {
    public UpgradeableWandRecipe(ResourceLocation id, JsonObject definition) { super(id, definition); }
    private WandParts.Cap match(CraftingContainer grid) {
        if (grid.getWidth()!=3 || grid.getHeight()!=3) return null;
        for (int i=0;i<9;i++) if (i!=2 && i!=4 && i!=6 && !grid.getItem(i).isEmpty()) return null;
        var rod=grid.getItem(4); var cap=grid.getItem(2);
        if (!ForgeRegistries.ITEMS.getKey(rod.getItem()).toString().equals("immersiveengineering:stick_treated") || cap.isEmpty() || !ItemStack.isSameItemSameTags(cap,grid.getItem(6))) return null;
        for (var part : WandParts.Cap.values()) {
            if (part.item!=null && ForgeRegistries.ITEMS.getKey(cap.getItem()).toString().equals(part.item)) return part;
            if (part.item==null) for (var component : WandComponent.all())
                if (component.legacyItem().equals("WandCap") && component.metadata()==part.metadata && cap.is(TCItems.wandComponent(component.path()))) return part;
        }
        return null;
    }
    @Override public boolean matches(CraftingContainer grid, Level level) { return thaumcraft.api.research.ResearchCategories.enabled(ArcaneEngineering.ID+":ROD_upgradeable") && match(grid)!=null; }
    @Override public boolean unlocked(CraftingContainer grid, Predicate<String> known) {
        var cap=match(grid);
        return cap!=null && known.test(ArcaneEngineering.ID+":ROD_upgradeable") && (cap.research().isEmpty() || known.test(cap.research()));
    }
    @Override public Map<Aspect,Integer> visCost(CraftingContainer grid) {
        var cap=match(grid); var result=new LinkedHashMap<Aspect,Integer>();
        if(cap!=null) for(var aspect:TCAspects.catalog().primals()) result.put(aspect,cap.craftCost*2);
        return result;
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registry) {
        var cap=match(grid); return cap==null ? ItemStack.EMPTY : ArcaneEngineering.WAND.get().create(WandParts.Rod.require(ArcaneEngineering.ID+":upgradeable"),cap,false,false);
    }
    @Override public RecipeSerializer<?> getSerializer() { return ArcaneEngineering.WAND_RECIPE.get(); }
    public static class Serializer implements RecipeSerializer<UpgradeableWandRecipe> {
        @Override public UpgradeableWandRecipe fromJson(ResourceLocation id, JsonObject value) { return new UpgradeableWandRecipe(id,value); }
        @Override public UpgradeableWandRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) { return fromJson(id,JsonParser.parseString(buffer.readUtf()).getAsJsonObject()); }
        @Override public void toNetwork(FriendlyByteBuf buffer, UpgradeableWandRecipe recipe) { buffer.writeUtf(recipe.displayDefinition().toString()); }
    }
}
