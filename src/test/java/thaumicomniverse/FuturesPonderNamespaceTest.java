package thaumicomniverse;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuturesPonderNamespaceTest {
    @Test void shippedBinaryScenesUseTheirRegisteredNamespace() throws Exception {
        for(String name:new String[]{"restored_altar","wireless"}){
            try(var in=new GZIPInputStream(Files.newInputStream(Path.of("src/main/resources/assets/thaumicomniverse/ponder/"+name+".nbt")))){
                String tags=new String(in.readAllBytes(),StandardCharsets.ISO_8859_1);
                assertFalse(tags.contains("thaumicomniverse:"),name+" still references unregistered legacy content");
                assertTrue(tags.contains("thaumicfutures:"),name+" lost its restored blocks");
            }
        }
    }
}
