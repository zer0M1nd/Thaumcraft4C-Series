package thmodern.plane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class StabilizerLeasesTest {
 @Test void repeatedRenewalDoesNotAddPhantomBeams(){var leases=new StabilizerLeases();leases.renew("a",0);leases.renew("a",5);leases.renew("b",5);assertEquals(2,leases.count(12));leases.remove("a");assertEquals(1,leases.count(12));leases.remove("a");assertEquals(1,leases.count(16));assertEquals(0,leases.count(17));}
 @Test void unloadedDevicesExpireIndependently(){var leases=new StabilizerLeases();for(int i=0;i<6;i++)leases.renew("device"+i,0);assertEquals(6,leases.count(5));leases.renew("device0",10);assertEquals(1,leases.count(12));assertEquals(0,leases.count(22));}
}
