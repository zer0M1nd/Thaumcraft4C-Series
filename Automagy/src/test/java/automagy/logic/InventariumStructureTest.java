package automagy.logic;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static automagy.logic.InventariumStructure.*;

class InventariumStructureTest {
    private static final Cell AIR=new Cell(true,true,false,false,0,false);
    private static final Cell AMBER=new Cell(true,false,true,false,0,false);
    private static final Cell CRYSTAL=new Cell(true,false,false,true,0,false);
    private static final Cell SHELF=new Cell(true,false,false,false,81,false);
    private final Map<List<Integer>,Cell> blocks=new HashMap<>();
    private void put(int x,int y,int z,Cell cell){blocks.put(List.of(x,y,z),cell);}
    private Cell at(int x,int y,int z){return blocks.getOrDefault(List.of(x,y,z),AIR);}
    private void construct(){
        put(0,-2,0,AMBER);
        for(int a:new int[]{-1,1})for(int b:new int[]{-2,2}){put(a,-1,b,SHELF);put(b,-1,a,SHELF);}
    }
    @Test void creationConsumesCentralCrystalButPreservesRequiredAirAndShelves(){
        construct();put(0,-1,0,CRYSTAL);
        assertEquals(Validity.VALID,validate(this::at,true));
        assertEquals(Validity.INVALID,validate(this::at,false));
        put(0,-1,0,AIR);assertEquals(Validity.VALID,validate(this::at,false));
        put(1,0,1,SHELF);assertEquals(Validity.INVALID,validate(this::at,false));
    }
    @Test void unloadedBoundaryDoesNotCountAsBrokenStructure(){
        construct();put(-2,-1,1,new Cell(false,false,false,false,0,false));
        assertEquals(Validity.UNLOADED,validate(this::at,false));
    }
    @Test void upgradesAreBoundedAndMemoryNeedsAdjacentAmberBelowCore(){
        construct();put(3,3,3,CRYSTAL);put(4,0,0,CRYSTAL);
        Cell memory=new Cell(true,false,false,false,0,true);
        put(1,-2,0,memory);put(0,-1,0,memory);put(-3,-3,-3,memory);
        Capacity c=capacity(this::at);
        assertEquals(648,c.slots());assertEquals(2,c.nodes());assertEquals(18,c.distance());assertEquals(1,c.memories());assertTrue(c.loaded());
        put(-4,-3,-3,AMBER);assertEquals(2,capacity(this::at).memories());
    }
    @Test void rangeIsSphericalAndIncludesExactBoundary(){
        assertTrue(inRange(6,8,0,10));assertFalse(inRange(7,8,0,10));
        assertFalse(inRange(Integer.MAX_VALUE,Integer.MAX_VALUE,0,10));
    }
}
