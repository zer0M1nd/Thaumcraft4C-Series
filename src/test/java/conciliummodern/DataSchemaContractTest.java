package conciliummodern;
import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataSchemaContractTest {
    @Test void everyEntityAspectFileUsesTheActualCoreDecoder()throws Exception{
        try(var paths=Files.list(Path.of("src/main/resources/data/thaumicconcilium/entity_aspects"))){
            var files=paths.filter(p->p.toString().endsWith(".json")).toList();assertFalse(files.isEmpty());
            for(var path:files)assertFalse(tc4revival.aspect.entity.EntityAspectJson.decode(JsonParser.parseString(Files.readString(path))).isEmpty(),path.toString());
        }
    }
}
