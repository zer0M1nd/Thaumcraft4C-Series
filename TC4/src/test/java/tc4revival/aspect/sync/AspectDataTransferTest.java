package tc4revival.aspect.sync;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectDataTransferTest {
    private static final String KEY="item_aspects/thaumcraft:base";
    @Test void totalSnapshotAbove32MiBStillSynchronizesOnlyTheSmallDifference(){
        String large="\""+"x".repeat(9*1024*1024)+"\"";var files=new TreeMap<String,String>();
        for(int i=0;i<4;i++)files.put("item_aspects/test:large_"+i,large);
        files.put(KEY,"{}");var server=new AspectDataSet(files);var local=new TreeMap<>(server.files());local.remove(KEY);
        var missing=AspectDataSet.missing(server.hashes(),local);assertEquals(List.of(KEY),missing);
        var delta=server.select(missing);assertEquals(2,AspectDataSet.transferBytes(delta));
        assertEquals(delta,AspectDataTransfer.decode(AspectDataTransfer.encode(delta).get(0)));
        assertEquals(server.hashes(),AspectDataSet.merge(server.hashes(),local,delta).hashes());
        assertThrows(IllegalArgumentException.class,()->AspectDataTransfer.encode(server.files()));
    }
    @Test void actualOversizedDownloadIsRejectedEvenWhenHighlyCompressible(){
        var payload=Map.of(KEY,"x".repeat(AspectDataSet.MAX_BYTES+1));
        assertThrows(IllegalArgumentException.class,()->AspectDataTransfer.encode(payload));
    }
    @Test void matchingBundledDataNeedsNoResourceDownload(){
        var server=new AspectDataSet(Map.of(KEY,"{\"format\":1}"));
        assertTrue(AspectDataSet.missing(server.hashes(),server.files()).isEmpty());
        assertEquals(server.files(),AspectDataSet.merge(server.hashes(),server.files(),Map.of()).files());
    }
    @Test void overridesAdditionsAndRemovalsFollowTheServerManifest(){
        var local=new AspectDataSet(Map.of(KEY,"{}","item_aspects/local:only","{}"));
        var server=new AspectDataSet(Map.of(KEY,"{\"changed\":true}","item_aspects/server:added","{}"));
        var missing=AspectDataSet.missing(server.hashes(),local.files());assertEquals(2,missing.size());
        var merged=AspectDataSet.merge(server.hashes(),local.files(),server.select(missing));
        assertEquals(server.files(),merged.files());assertFalse(merged.files().containsKey("item_aspects/local:only"));
    }
    @Test void sameNameWithWrongContentsCannotSatisfyTheSnapshot(){
        var server=new AspectDataSet(Map.of(KEY,"{}"));
        assertThrows(IllegalArgumentException.class,()->AspectDataSet.merge(server.hashes(),Map.of(KEY,"[]"),Map.of()));
        assertThrows(IllegalArgumentException.class,()->AspectDataSet.merge(server.hashes(),Map.of(),Map.of()));
    }
    @Test void onlyWhitelistedDataPathsAreAccepted(){
        for(String key:List.of("recipes/thaumcraft:base","item_aspects/thaumcraft:../secrets","item_aspects/thaumcraft:/absolute","item_aspects/thaumcraft:foo//bar","item_aspects/THAUMCRAFT:bad"))
            assertThrows(IllegalArgumentException.class,()->AspectDataSet.validateKey(key));
    }
    @Test void multiFrameCompressionRoundTripsExactly(){
        byte[] random=new byte[250000];new Random(18).nextBytes(random);
        var values=Map.of(KEY,Base64.getEncoder().encodeToString(random));var chunks=AspectDataTransfer.encode(values);
        assertTrue(chunks.size()>1);var receiver=new AspectDataTransfer.Receiver();
        for(int i=0;i<chunks.size();i++){
            var result=receiver.accept(i,chunks.size(),chunks.get(i));
            if(i+1<chunks.size())assertTrue(result.isEmpty());else assertEquals(values,result.orElseThrow());
        }
    }
    @Test void outOfOrderDuplicateAndOversizedFramesAreRejected(){
        var receiver=new AspectDataTransfer.Receiver();
        assertThrows(IllegalArgumentException.class,()->receiver.accept(1,2,new byte[1]));
        receiver.accept(0,2,new byte[1]);
        assertThrows(IllegalArgumentException.class,()->receiver.accept(0,2,new byte[1]));
        assertThrows(IllegalArgumentException.class,()->new AspectDataTransfer.Receiver().accept(0,257,new byte[1]));
        assertThrows(IllegalArgumentException.class,()->new AspectDataTransfer.Receiver().accept(0,1,new byte[AspectDataTransfer.CHUNK+1]));
    }
    @Test void corruptCompressedDataFailsWithoutCreatingASnapshot(){
        assertThrows(IllegalArgumentException.class,()->AspectDataTransfer.decode(new byte[]{1,2,3}));
        var chunks=AspectDataTransfer.encode(Map.of(KEY,"{}"));var chunk=chunks.get(0);
        assertThrows(IllegalArgumentException.class,()->AspectDataTransfer.decode(Arrays.copyOf(chunk,chunk.length-3)));
    }
}
