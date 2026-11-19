package thmodern.vat;

import java.util.*;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngredientAssignmentTest {
    @Test void broadTagsDoNotStealARequiredExactItem(){
        List<Predicate<String>> ingredients=List.of(s->s.endsWith("wood"),s->s.equals("greatwood"));
        assertArrayEquals(new int[]{1,0},IngredientAssignment.match(ingredients,List.of("greatwood","silverwood")).orElseThrow());
    }
    @Test void duplicateIngredientsRequireSeparatePedestals(){
        List<Predicate<String>> ingredients=List.of(s->s.equals("diamond"),s->s.equals("diamond"));
        assertTrue(IngredientAssignment.match(ingredients,List.of("diamond")).isEmpty());
        assertTrue(IngredientAssignment.match(ingredients,List.of("diamond","coal")).isEmpty());
        assertTrue(IngredientAssignment.match(ingredients,List.of("diamond","diamond")).isPresent());
    }
    @Test void unrelatedExtraPedestalPreventsStarting(){assertTrue(IngredientAssignment.match(List.<Predicate<String>>of(s->true),List.of("a","b")).isEmpty());}
    @Test void augmentingPathsCanReassignSeveralPreviousChoices(){
        List<Predicate<Integer>> ingredients=List.of(n->n==1||n==2,n->n==2||n==3,n->n==1);
        assertArrayEquals(new int[]{1,2,0},IngredientAssignment.match(ingredients,List.of(1,2,3)).orElseThrow());
    }
}
