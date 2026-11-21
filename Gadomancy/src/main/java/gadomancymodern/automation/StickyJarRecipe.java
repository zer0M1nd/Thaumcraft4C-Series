package gadomancymodern.automation;

import com.google.gson.*;
import gadomancymodern.Gadomancy;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.ArcaneRecipe;

public class StickyJarRecipe extends ArcaneRecipe {
    protected final JsonObject raw;
    public StickyJarRecipe(ResourceLocation id,JsonObject json){super(id,json);raw=json.deepCopy();}
    public static boolean jar(ItemStack stack){return stack.getItem() instanceof tc4revival.content.EssentiaJarItem||stack.getItem() instanceof gadomancymodern.item.RemoteJarItem;}
    @Override public boolean matches(CraftingContainer grid,Level level){
        if(!super.matches(grid,level))return false;
        for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(jar(stack))return !stack.hasTag()||!stack.getTag().getBoolean("isStickyJar");}return false;
    }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){
        for(int i=0;i<grid.getContainerSize();i++)if(jar(grid.getItem(i))){var result=grid.getItem(i).copyWithCount(1);result.getOrCreateTag().putBoolean("isStickyJar",true);return result;}
        return ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer(){return Gadomancy.STICKY_RECIPE.get();}
    public static class Serializer implements RecipeSerializer<StickyJarRecipe>{
        public StickyJarRecipe fromJson(ResourceLocation id,JsonObject json){return new StickyJarRecipe(id,json);}
        public StickyJarRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return fromJson(id,JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf buffer,StickyJarRecipe recipe){buffer.writeUtf(recipe.raw.toString(),1048576);}
    }
}
