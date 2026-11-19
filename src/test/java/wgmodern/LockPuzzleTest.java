package wgmodern;
import java.util.Arrays;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
public class LockPuzzleTest {
 @Test public void allSixteenAuthorPuzzlesArePermutationsAndSolvable(){assertEquals(16,LockPuzzle.PRESETS.length);for(int n=0;n<16;n++){var b=LockPuzzle.preset(n);assertEquals(9,Arrays.stream(b).distinct().count());assertEquals(8,b[8]);int inversions=0;for(int i=0;i<9;i++)for(int j=i+1;j<9;j++)if(b[i]!=8&&b[j]!=8&&b[i]>b[j])inversions++;assertEquals(0,inversions%2,"Original puzzle "+n+" must be solvable on a 3x3 board");assertFalse(LockPuzzle.solved(b));}}
 @Test public void onlyAdjacentTilesCanMoveAndCompletionNeedsEveryTile(){int[] b={0,1,2,3,4,5,6,8,7};assertFalse(LockPuzzle.move(b,-1));assertFalse(LockPuzzle.move(b,9));assertFalse(LockPuzzle.move(b,0));assertFalse(LockPuzzle.move(b,7));assertTrue(LockPuzzle.move(b,8));assertTrue(LockPuzzle.solved(b));int[] wrap={0,1,8,2,3,4,5,6,7};assertFalse(LockPuzzle.move(wrap,3));assertFalse(LockPuzzle.solved(new int[]{0,1,2}));}
}
