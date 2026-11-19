package automagy;
import automagy.logic.BoilerOutput;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BoilerOutputTest {
    static class Vessel implements BoilerOutput.Vessel<String>{String type,filter;int n,capacity=64;Vessel(String type,int n){this.type=type;this.n=n;}public String aspect(){return type;}public String filter(){return filter;}public int amount(){return n;}public boolean acceptOne(String a){if(n>=capacity||filter!=null&&!filter.equals(a)||n>0&&!Objects.equals(type,a))return false;type=a;n++;return true;}}
    @Test void fillsTypedColumnsThenOnlyOneEmptyVesselWithoutDoubleSpending(){
        var buffer=new LinkedHashMap<>(Map.of("water",4,"fire",2));var a=new Vessel("water",1);var b=new Vessel("water",2);var c=new Vessel(null,0);var d=new Vessel(null,0);
        assertEquals(3,BoilerOutput.distribute(buffer,List.of(a,b,c,d),new Random(1)));assertEquals(2,buffer.get("water"));assertEquals(1,buffer.get("fire"));assertEquals("fire",c.type);assertEquals(0,d.n);
    }
    @Test void rejectedOutputRetainsStockAndFilteredEmptyCanReceiveExcludedType(){
        var buffer=new HashMap<>(Map.of("water",3));var full=new Vessel("water",64);var typed=new Vessel("water",1);var empty=new Vessel(null,0);empty.filter="water";
        assertEquals(2,BoilerOutput.distribute(buffer,List.of(full,typed,empty),new Random(1)));assertEquals(1,buffer.get("water"));assertEquals(64,full.n);
    }
}
