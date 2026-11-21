package automagy;
import automagy.logic.ConnectedContainers;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConnectedContainersTest {
    @Test void cyclesAndDuplicateSeedsRespectCapacityAndTraversalOrder(){
        var edges=Map.of(1,List.of(2,3),2,List.of(1,4),3,List.of(1),4,List.of(2));
        assertEquals(List.of(1,2,4),ConnectedContainers.discover(List.of(1,1,3),3,edges::containsKey,edges::get));
    }
    @Test void inaccessibleContainersAreBarriersAndDoNotConsumeBudget(){
        assertEquals(List.of(0,1,8,9),ConnectedContainers.discover(List.of(0,8),4,n->n!=2,n->List.of(n+1)));
    }
    @Test void hugeConnectedGroupsAreBoundedWithoutRecursion(){
        assertEquals(10000,ConnectedContainers.discover(List.of(0),10000,n->true,n->List.of(n+1)).size());
    }
}
