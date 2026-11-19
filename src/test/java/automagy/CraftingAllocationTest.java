package automagy;
import automagy.logic.CraftingAllocation;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CraftingAllocationTest {
 @Test void repeatedCellsCannotSpendOneItemTwice(){assertTrue(CraftingAllocation.allocate(List.of("plank","plank"),List.of("plank"),new int[]{1},String::equals).isEmpty());var a=CraftingAllocation.allocate(List.of("plank","plank"),List.of("plank"),new int[]{2},String::equals).orElseThrow();assertArrayEquals(new int[]{2},a.slotUse());}
 @Test void broadMatchDoesNotStealTheOnlyExactIngredient(){var a=CraftingAllocation.allocate(List.of("wood","oak"),List.of("oak","birch"),new int[]{1,1},(wanted,actual)->wanted.equals("wood")||wanted.equals(actual)).orElseThrow();assertArrayEquals(new int[]{1,0},a.gridSources());}
 @Test void emptyCellsAndDefensiveCopiesArePreserved(){var a=CraftingAllocation.allocate(Arrays.asList(null,"iron",null),List.of("iron"),new int[]{1},String::equals).orElseThrow();assertArrayEquals(new int[]{-1,0,-1},a.gridSources());a.slotUse()[0]=99;assertEquals(1,a.slotUse()[0]);}
}
