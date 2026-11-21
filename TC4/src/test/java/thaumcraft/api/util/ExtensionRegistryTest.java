package thaumcraft.api.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExtensionRegistryTest {
    @Test void readsAreStableSnapshotsWithDeterministicOrder(){
        var r=new ExtensionRegistry<Integer>();r.register("test:z",0,1);var before=r.entries();
        r.register("test:b",2,2);r.register("test:a",2,3);
        assertEquals(java.util.List.of(3,2,1),r.entries().stream().map(ExtensionRegistry.Entry::value).toList());
        assertEquals(1,before.size());
        assertThrows(IllegalArgumentException.class,()->r.register("test:a",99,4));
        assertThrows(UnsupportedOperationException.class,()->r.entries().clear());
    }
}
