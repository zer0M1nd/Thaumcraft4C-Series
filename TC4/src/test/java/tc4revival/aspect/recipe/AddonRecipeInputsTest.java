package tc4revival.aspect.recipe;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.item.LegacyItemIdentity;
import static org.junit.jupiter.api.Assertions.*;
class AddonRecipeInputsTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void mergeFeedsExistingFormulaAndModernSemanticIdentity() {
        var input=new RecipeAspectData.Key("minecraft:iron",0);var output=new RecipeAspectData.Key("addon:gear",0);
        var base=new RecipeAspectData(Map.of("minecraft:iron",new RecipeAspectData.ItemProperties(false,true,null)),List.of(new RecipeAspectData.Registration(input,Map.of("metallum",8))),List.of());
        var addon=new RecipeAspectData(Map.of("addon:gear",new RecipeAspectData.ItemProperties(false,true,null)),List.of(),
            List.of(new RecipeAspectData.Recipe(RecipeAspectData.Kind.CRAFTING,output,1,List.of(RecipeAspectData.Ingredient.direct(input)),Map.of())));
        var merged=RecipeAspectInputs.merge(List.of(base,addon));
        assertEquals(6,new RecipeAspectEngine(merged,catalog).object(output).amount(catalog.require("metallum")));
        var identity=new LegacyItemIdentity(Map.of(),Map.of(),merged);
        assertEquals(output,identity.recipeKey("addon:gear"));assertEquals("addon:gear:0".hashCode(),identity.scanHash(output));
        assertEquals(1,base.items().size());assertNull(new LegacyItemIdentity(Map.of(),Map.of(),base).recipeKey("addon:gear"));
    }
    @Test void rejectConflictingPropertiesWithoutReplacingBaseline() {
        var first=new RecipeAspectData(Map.of("a:b",new RecipeAspectData.ItemProperties(false,true,null)),List.of(),List.of());
        var other=new RecipeAspectData(Map.of("a:b",new RecipeAspectData.ItemProperties(true,false,null)),List.of(),List.of());
        assertThrows(IllegalArgumentException.class,()->RecipeAspectInputs.merge(List.of(first,other)));
    }
}
