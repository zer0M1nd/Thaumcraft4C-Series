package tc4revival.aspect.inference;

import com.google.gson.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class InferenceFilesTest {
    @TempDir Path directory;
    @Test void backgroundExportsAreOrderedAndLeaveCompleteJson() throws Exception {
        var first=new JsonObject();var rows=new JsonArray();
        for(int i=0;i<10000;i++){var row=new JsonObject();row.addProperty("key","item:test:variant_"+i);rows.add(row);}
        first.add("recipes",rows);
        var second=new JsonObject();second.addProperty("generation",2);
        var a=InferenceFiles.writeAsync(directory,"index.json",first);
        var b=InferenceFiles.writeAsync(directory,"index.json",second);
        a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
        assertEquals(second,JsonParser.parseString(Files.readString(directory.resolve("index.json"))));
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void failedDestinationDoesNotPoisonLaterExportsOrLeaveTemporaryFiles() throws Exception {
        Files.createDirectory(directory.resolve("blocked.json"));
        Files.writeString(directory.resolve("blocked.json/keep"),"original");
        var data=new JsonObject();data.addProperty("key","preserved");
        assertThrows(java.util.concurrent.ExecutionException.class,()->
                InferenceFiles.writeAsync(directory,"blocked.json",data).get(10,TimeUnit.SECONDS));
        InferenceFiles.writeAsync(directory,"valid.json",data).get(10,TimeUnit.SECONDS);
        assertEquals("original",Files.readString(directory.resolve("blocked.json/keep")));
        assertEquals(data,JsonParser.parseString(Files.readString(directory.resolve("valid.json"))));
        try(var files=Files.list(directory)){assertFalse(files.anyMatch(p->p.getFileName().toString().startsWith("writing-")));}
    }
}
