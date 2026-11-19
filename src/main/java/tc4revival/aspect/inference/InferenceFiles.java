package tc4revival.aspect.inference;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;

/** Editable JSON files; atomic replacement keeps an interrupted scan from destroying its seeds. */
public class InferenceFiles {
    private static final java.util.concurrent.ExecutorService WRITER = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "TC4 inference index writer"); thread.setDaemon(true); return thread;
    });
    public static Path directory(){return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("thaumcraft/advanced-inference");}
    public static JsonObject read(String name) throws IOException {
        Path path=directory().resolve(name);
        if(!Files.isRegularFile(path))return name.equals("keyword-fallback.json")?defaultKeywords():new JsonObject();
        try(var reader=Files.newBufferedReader(path)){return JsonParser.parseReader(reader).getAsJsonObject();}
    }
    public static void write(String name,JsonObject json) throws IOException {
        write(directory(),name,json);
    }
    /** The caller publishes a finished JSON snapshot and must not mutate it afterwards.
     * Serial writes keep an older export from replacing a newer queued export. */
    public static java.util.concurrent.CompletableFuture<Void> writeAsync(Path directory,String name,JsonObject json) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { write(directory,name,json); }
            catch(IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
        },WRITER);
    }
    static void write(Path directory,String name,JsonObject json) throws IOException {
        Files.createDirectories(directory);Path destination=directory.resolve(name),temp=Files.createTempFile(directory,"writing-",".json");
        try{try(var writer=Files.newBufferedWriter(temp)){new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json,writer);}
            try{Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    public static void defaults() throws IOException {
        for(String name:java.util.List.of("item-fallback.json","verify.json"))if(!Files.exists(directory().resolve(name)))write(name,new JsonObject());
        if(!Files.exists(directory().resolve("keyword-fallback.json"))){
            write("keyword-fallback.json",defaultKeywords());
        }
    }
    public static JsonObject defaultKeywords(){
            var rules=new JsonArray();
            rule(rules,"sapling|树苗", "arbor",1,"herba",2);rule(rules,"leaves|树叶", "herba",1,"aer",1);
            rule(rules,"planks|木板", "arbor",1);rule(rules,"(?:^|[._:/ ])log(?:$|[._:/ ])|原木", "arbor",4);
            rule(rules,"wood.*slab|木.*台阶", "arbor",1);rule(rules,"wood.*stairs|木.*楼梯", "arbor",1);
            rule(rules,"fence|栅栏", "arbor",1);rule(rules,"(?:^|[._:/ ])(?:stone|cobblestone|stonebrick)(?:$|[._:/ ])|石砖|石头|圆石", "terra",1);
            var root=new JsonObject();root.add("rules",rules);return root;
    }
    private static void rule(JsonArray rules,String pattern,Object... aspects){var row=new JsonObject();row.addProperty("pattern",pattern);var values=new JsonObject();for(int i=0;i<aspects.length;i+=2)values.addProperty((String)aspects[i],(Integer)aspects[i+1]);row.add("aspects",values);rules.add(row);}
    private InferenceFiles(){}
}
