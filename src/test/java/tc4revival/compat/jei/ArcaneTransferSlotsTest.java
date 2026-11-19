package tc4revival.compat.jei;
import java.util.*;
import java.lang.reflect.Proxy;
import mezz.jei.api.gui.ingredient.*;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArcaneTransferSlotsTest {
    private static IRecipeSlotView slot(String name,RecipeIngredientRole role){
        return (IRecipeSlotView)Proxy.newProxyInstance(IRecipeSlotView.class.getClassLoader(),new Class<?>[]{IRecipeSlotView.class},(p,m,a)->switch(m.getName()){
            case "getSlotName"->Optional.of(name);case "getRole"->role;
            case "getAllIngredients"->java.util.stream.Stream.empty();
            case "getDisplayedIngredient"->Optional.empty();default->null;
        });
    }
    @Test void transferKeepsEmptyGridPositionsButExcludesVisAndOutput(){
        var grid=new ArrayList<IRecipeSlotView>();for(int i=0;i<9;i++)grid.add(slot("items/"+i,RecipeIngredientRole.INPUT));
        var full=new ArrayList<>(grid);for(int i=0;i<6;i++)full.add(slot("cost/"+i,RecipeIngredientRole.INPUT));
        full.add(slot("output",RecipeIngredientRole.OUTPUT));
        full.add(slot("research",RecipeIngredientRole.INPUT));
        var selected=ArcaneTransferSlots.items(()->full);
        assertEquals(9,selected.size());for(int i=0;i<9;i++)assertSame(grid.get(i),selected.get(i));
    }
    @Test void unnamedBridgeInputsCannotEraseEmptyGridPositions(){
        List<List<net.minecraft.world.item.ItemStack>> grid=Collections.nCopies(9,List.of());
        var result=ArcaneTransferSlots.items(()->List.of(slot("",RecipeIngredientRole.INPUT)),grid);
        assertEquals(9,result.size());assertTrue(result.get(0).isEmpty());assertTrue(result.get(8).isEmpty());
        for(int i=0;i<9;i++){assertEquals(Optional.of("items/"+i),result.get(i).getSlotName());assertEquals(RecipeIngredientRole.INPUT,result.get(i).getRole());}
    }
}
