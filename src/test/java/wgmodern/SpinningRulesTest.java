package wgmodern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SpinningRulesTest {
    @Test public void duplicateInputsCannotReuseOneIngredient(){assertFalse(SpinningRecipe.matchMatrix(new boolean[][]{{true,false},{true,false}}));}
    @Test public void overlappingTagsBacktrackToValidAssignment(){assertTrue(SpinningRecipe.matchMatrix(new boolean[][]{{true,true},{true,false}}));}
    @Test public void allPermutationsRemainValid(){for(int n=1;n<=5;n++){boolean[][] matrix=new boolean[n][n];for(int i=0;i<n;i++)matrix[i][n-i-1]=true;assertTrue(SpinningRecipe.matchMatrix(matrix));}}
    @Test public void unmatchedExtraIngredientFails(){assertFalse(SpinningRecipe.matchMatrix(new boolean[][]{{true,false,false},{false,true,false},{false,false,false}}));}
    @Test public void emptyCraftingGridCannotProduceThread(){assertFalse(SpinningRecipe.matchMatrix(new boolean[0][0]));}
}
