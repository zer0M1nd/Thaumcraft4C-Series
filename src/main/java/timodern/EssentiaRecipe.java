package timodern;

import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Four identical filled phials / essence blocks; the aspect is preserved, including addon aspects. */
public class EssentiaRecipe extends tc4revival.content.ArcaneRecipe {
    private final int stage;
    public EssentiaRecipe(ResourceLocation id,JsonObject json){super(id,json);stage=json.get("stage").getAsInt();if(stage<0||stage>2)throw new IllegalArgumentException("Invalid essence recipe stage");}
    private String aspect(ItemStack stack){
        if(stage==0){if(!stack.is(tc4revival.content.TCItems.ESSENTIA_PHIAL.get()))return "";var stored=((tc4revival.content.StoredAspectItem)stack.getItem()).storedAspects(stack);return stored.size()==1&&stored.amount(stored.aspects().get(0))==8?stored.aspects().get(0).tag():"";}
        return stack.is(stage==1?ThaumicInfusion.ESSENTIA.get().asItem():ThaumicInfusion.BRICKS.get().asItem())?EssentiaBlock.aspect(stack):"";
    }
    @Override public boolean matches(CraftingContainer grid,Level level){if(!super.matches(grid,level))return false;String type="";int count=0;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;String a=aspect(stack);if(a.isEmpty()||!type.isEmpty()&&!type.equals(a))return false;type=a;count++;}return count==4;}
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){for(int i=0;i<grid.getContainerSize();i++)if(!grid.getItem(i).isEmpty())return EssentiaBlock.stack(stage==0?ThaumicInfusion.ESSENTIA.get():stage==1?ThaumicInfusion.BRICKS.get():ThaumicInfusion.CHISELED.get(),aspect(grid.getItem(i)));return ItemStack.EMPTY;}
    @Override public RecipeSerializer<?> getSerializer(){return ThaumicInfusion.ESSENTIA_RECIPE.get();}
    @Override public JsonObject displayDefinition(){
        var json=super.displayDefinition();
        var sample=stage==0?tc4revival.content.EssentiaPhialItem.filled(tc4revival.aspect.TCAspects.catalog().require("aer"))
                :EssentiaBlock.stack(stage==1?ThaumicInfusion.ESSENTIA.get():ThaumicInfusion.BRICKS.get(),"aer");
        var inputs=new JsonArray();for(int i=0;i<4;i++)inputs.add(sample.save(new net.minecraft.nbt.CompoundTag()).toString());
        json.add("display_inputs",inputs);json.addProperty("display_width",2);return json;
    }
    public static class Serializer implements RecipeSerializer<EssentiaRecipe>{
        public EssentiaRecipe fromJson(ResourceLocation id,JsonObject json){return new EssentiaRecipe(id,json);}
        public EssentiaRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf(65536)).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf b,EssentiaRecipe r){b.writeUtf(r.displayDefinition().toString(),65536);}
    }
}
