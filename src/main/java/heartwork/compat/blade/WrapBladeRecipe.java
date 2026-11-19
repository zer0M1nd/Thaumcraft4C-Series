package heartwork.compat.blade;

import com.google.gson.JsonObject;
import heartwork.Heartwork;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class WrapBladeRecipe extends ShapedRecipe {
    private final ShapedRecipe shape;
    private final ResourceLocation texture;
    private final float attack;
    public WrapBladeRecipe(ShapedRecipe shape,ResourceLocation texture,float attack) {
        super(shape.getId(),shape.getGroup(),shape.category(),shape.getWidth(),shape.getHeight(),shape.getIngredients(),ItemStack.EMPTY);
        this.shape=shape;this.texture=texture;this.attack=attack;
    }
    @Override public boolean matches(CraftingContainer grid,Level level) {
        if(grid.getWidth()!=3||grid.getHeight()!=3)return false;
        for(int i=0;i<9;i++)if(!getIngredients().get(i).test(grid.getItem(i)))return false;
        return WrappedBladeItem.wrapped(grid.getItem(4)).isEmpty();
    }
    private ItemStack wrap(ItemStack source) {
        if(source.isEmpty())return ItemStack.EMPTY;
        var result=new ItemStack(Heartwork.item("wrapped_blade"));
        result.getOrCreateTag().put("WrapItem",source.copyWithCount(1).save(new CompoundTag()));
        result.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> {
            s.setNonEmpty();s.setSealed(false);s.setMaxDamage(Math.max(1,source.getMaxDamage()));s.setBaseAttackModifier(attack);
            s.setModel(new ResourceLocation("heartwork:blade/blade.obj"));s.setTexture(texture);
            result.getOrCreateTag().put("bladeState",s.serializeNBT());
        });
        EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(source),result);
        result.setHoverName(Component.translatable("item.heartwork.wrapped_name",source.getHoverName()));return result;
    }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access) {return wrap(grid.getItem(6));}
    @Override public ItemStack getResultItem(RegistryAccess access) {
        var candidates=getIngredients().get(6).getItems();return candidates.length==0?ItemStack.EMPTY:wrap(candidates[0]);
    }
    @Override public RecipeSerializer<?> getSerializer() {return BladeCompat.WRAPPING.get();}
    public static class Serializer implements RecipeSerializer<WrapBladeRecipe> {
        @Override public WrapBladeRecipe fromJson(ResourceLocation id,JsonObject json) {
            return new WrapBladeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id,json),new ResourceLocation(GsonHelper.getAsString(json,"texture")),GsonHelper.getAsFloat(json,"attack"));
        }
        @Override public WrapBladeRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            return new WrapBladeRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id,buffer),buffer.readResourceLocation(),buffer.readFloat());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,WrapBladeRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer,recipe.shape);buffer.writeResourceLocation(recipe.texture);buffer.writeFloat(recipe.attack);
        }
    }
}
