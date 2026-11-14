package advancedthaumaturgy;
import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.*;
import tc4revival.aspect.*;
/** Original two-cap assembly supports all registered caps without host recipe overlap. */
public class MercurialAssemblyRecipe extends ArcaneRecipe {
    public MercurialAssemblyRecipe(ResourceLocation id,JsonObject json){super(id,json);}
    private WandParts.Cap cap(CraftingContainer grid){
        if(grid.getWidth()!=3||grid.getHeight()!=3||!grid.getItem(4).is(AdvancedThaumaturgy.MERCURIAL_ROD.get()))return null;
        for(int i:new int[]{0,1,3,5,7,8})if(!grid.getItem(i).isEmpty())return null;
        var a=WandbenchEntity.cap(grid.getItem(2));var b=WandbenchEntity.cap(grid.getItem(6));return a!=null&&a==b?a:null;
    }
    @Override public boolean matches(CraftingContainer grid,Level level){return ATConfig.enabled("mercurial_wand")&&cap(grid)!=null;}
    @Override public boolean unlocked(CraftingContainer grid,java.util.function.Predicate<String> research){return research.test("advancedthaumaturgy:MERCURIALWAND");}
    @Override public Map<Aspect,Integer> visCost(CraftingContainer grid){Map<Aspect,Integer> result=new LinkedHashMap<>();var cap=cap(grid);if(cap!=null)TCAspects.catalog().primals().forEach(a->result.put(a,10+cap.craftCost));return result;}
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){var cap=cap(grid);return cap==null?ItemStack.EMPTY:AdvancedThaumaturgy.MERCURIAL_WAND.get().create(WandParts.Rod.require(MercurialWandItem.ROD),cap,false,false);}
    @Override public RecipeSerializer<?> getSerializer(){return AdvancedThaumaturgy.WAND_ASSEMBLY.get();}
    public static class Serializer implements RecipeSerializer<MercurialAssemblyRecipe>{
        public MercurialAssemblyRecipe fromJson(ResourceLocation id,JsonObject json){return new MercurialAssemblyRecipe(id,json);}
        public MercurialAssemblyRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf()).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf b,MercurialAssemblyRecipe r){b.writeUtf(r.displayDefinition().toString());}
    }
}
