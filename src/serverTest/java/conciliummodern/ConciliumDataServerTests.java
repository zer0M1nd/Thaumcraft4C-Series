package conciliummodern;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumDataServerTests {
    @GameTest(template="empty") public static void allEnabledPackRecipesAreActuallyLoaded(GameTestHelper test){
        int checked=0;var missing=new java.util.ArrayList<String>();
        for(var entry:test.getLevel().getServer().getResourceManager().listResources("recipes",id->id.getNamespace().equals("thaumicconcilium")&&id.getPath().endsWith(".json")).entrySet()){
            try(var reader=entry.getValue().openAsReader()){
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(!CraftingHelper.processConditions(json,"conditions",ICondition.IContext.EMPTY))continue;
                String path=entry.getKey().getPath();var recipe=ResourceLocation.fromNamespaceAndPath("thaumicconcilium",path.substring(8,path.length()-5));checked++;
                if(test.getLevel().getRecipeManager().byKey(recipe).isEmpty())missing.add(recipe.toString());
            }catch(java.io.IOException error){throw new RuntimeException(error);}
        }
        test.assertTrue(checked>30,"Actual pack exposes the complete base recipe set");test.assertTrue(missing.isEmpty(),"Enabled recipes absent from real RecipeManager: "+missing);test.succeed();
    }
}
