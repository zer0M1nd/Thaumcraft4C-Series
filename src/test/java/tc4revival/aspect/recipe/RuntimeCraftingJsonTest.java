package tc4revival.aspect.recipe;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.recipe.RecipeAspectData.*;
import static org.junit.jupiter.api.Assertions.*;
class RuntimeCraftingJsonTest {
    @Test void sharedTagAlternativesRoundTripWithoutChangingCandidateOrder(){
        var items=new LinkedHashMap<String,ItemProperties>();var alternatives=new ArrayList<Key>();
        for(int i=0;i<200;i++){String id="test:material_"+i;items.put(id,new ItemProperties(false,true,null));alternatives.add(new Key(id,0));}
        Collections.reverse(alternatives);items.put("test:result",new ItemProperties(false,true,null));
        var input=new Ingredient(alternatives,true);var rows=new ArrayList<Recipe>();
        for(int i=0;i<100;i++)rows.add(new Recipe(Kind.CRAFTING,new Key("test:result",0),i+1,Collections.nCopies(9,input),Map.of()));
        var data=new RecipeAspectData(items,List.of(),rows);var compact=RuntimeCraftingJson.encode(data);
        assertEquals(1,compact.getAsJsonArray("ingredients").size());
        assertEquals(data,RecipeAspectJson.decode(compact));
        assertTrue(compact.toString().length()*20<RecipeAspectJson.encode(data).toString().length());
        assertEquals(compact,RuntimeCraftingJson.encode(RecipeAspectJson.decode(compact)));
    }
    @Test void legacyFormatRemainsReadableAndNonModernInputIsNotSilentlyDropped(){
        var key=new Key("test:legacy",4);var data=new RecipeAspectData(Map.of(key.item(),new ItemProperties(false,true,null)),List.of(),
                List.of(new Recipe(Kind.ARCANE,key,1,List.of(Ingredient.direct(key)),Map.of("aer",2))));
        assertEquals(data,RecipeAspectJson.decode(RecipeAspectJson.encode(data)));
        assertThrows(IllegalArgumentException.class,()->RuntimeCraftingJson.encode(data));
    }
}
