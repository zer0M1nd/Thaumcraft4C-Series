package heartwork.compat.blade;

import com.google.gson.JsonObject;
import java.util.HashMap;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/** Awakening inherits one blade; the original RecipeBx inherits only slot (1,1). */
public class HeartworkBladeRecipe extends ShapedRecipe {
    private final ResourceLocation blade;
    private final String inheritance;
    private final boolean fixed;
    private final ShapedRecipe shape;
    public HeartworkBladeRecipe(ShapedRecipe shape, ResourceLocation blade, String inheritance, boolean fixed) {
        super(shape.getId(),shape.getGroup(),shape.category(),shape.getWidth(),shape.getHeight(),shape.getIngredients(),ItemStack.EMPTY);
        this.blade=blade;this.inheritance=inheritance;this.fixed=fixed;
        this.shape=shape;
    }
    @Override public boolean matches(CraftingContainer grid,Level level) {
        if (!fixed) return super.matches(grid,level);
        if (grid.getWidth()!=getWidth() || grid.getHeight()!=getHeight()) return false;
        for (int i=0;i<grid.getContainerSize();i++) if (!getIngredients().get(i).test(grid.getItem(i))) return false;
        return true;
    }
    @Override public ItemStack getResultItem(RegistryAccess access) {
        var definition=access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY).get(blade);
        return definition==null ? ItemStack.EMPTY : definition.getBlade();
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        ItemStack result=getResultItem(access);
        if(result.isEmpty() || inheritance.equals("none")) return result;
        ItemStack source=ItemStack.EMPTY;
        if(inheritance.equals("center")) source=grid.getItem(4);
        else for(int i=0;i<grid.getContainerSize();i++) if(grid.getItem(i).getItem() instanceof ItemSlashBlade) { source=grid.getItem(i);break; }
        var from=source.getCapability(ItemSlashBlade.BLADESTATE).resolve();
        var to=result.getCapability(ItemSlashBlade.BLADESTATE).resolve();
        if(from.isPresent() && to.isPresent()) {
            var old=from.get();var next=to.get();
            next.setKillCount(old.getKillCount());next.setProudSoulCount(old.getProudSoulCount());
            next.setRefine(old.getRefine());next.setAdjust(old.getAdjust());
            result.getOrCreateTag().put("bladeState",next.serializeNBT());
        }
        var enchants=new HashMap<>(EnchantmentHelper.getEnchantments(result));
        for(var entry:EnchantmentHelper.getEnchantments(source).entrySet()) {
            var enchant=entry.getKey();
            if(!enchant.canEnchant(result) || enchants.keySet().stream().anyMatch(other -> other!=enchant && !enchant.isCompatibleWith(other))) continue;
            enchants.put(enchant,Math.min(enchant.getMaxLevel(),Math.max(entry.getValue(),enchants.getOrDefault(enchant,0))));
        }
        EnchantmentHelper.setEnchantments(enchants,result);
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return BladeCompat.CRAFTING.get(); }
    public static class Serializer implements RecipeSerializer<HeartworkBladeRecipe> {
        @Override public HeartworkBladeRecipe fromJson(ResourceLocation id,JsonObject json) {
            JsonObject copy=json.deepCopy(), result=new JsonObject();result.addProperty("item","slashblade:slashblade");copy.add("result",result);
            return new HeartworkBladeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id,copy),new ResourceLocation(GsonHelper.getAsString(json,"blade")),
                    GsonHelper.getAsString(json,"inherit","first"),GsonHelper.getAsBoolean(json,"fixed",false));
        }
        @Override public HeartworkBladeRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            return new HeartworkBladeRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id,buffer),buffer.readResourceLocation(),buffer.readUtf(),buffer.readBoolean());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,HeartworkBladeRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer,recipe.shape);buffer.writeResourceLocation(recipe.blade);buffer.writeUtf(recipe.inheritance);buffer.writeBoolean(recipe.fixed);
        }
    }
}
