package tc4revival.research;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Optional;

/** Local navigation preferences, separated by world/server and player. Never stores research permissions. */
public class BookLocationStore {
    private final Path file;
    public BookLocationStore(Path file){this.file=file;}
    private JsonObject read() throws IOException {
        if(!Files.isRegularFile(file))return new JsonObject();
        try {var value=JsonParser.parseString(Files.readString(file));return value.isJsonObject()?value.getAsJsonObject():new JsonObject();}
        catch(JsonParseException invalid){return new JsonObject();}
    }
    public Optional<ResearchBookNavigation.Location> load(String world) throws IOException {
        try {var data=read().getAsJsonObject(world);if(data==null)return Optional.empty();
            var location=new ResearchBookNavigation.Location(data.get("category").getAsString(),data.get("x").getAsDouble(),data.get("y").getAsDouble());
            return Double.isFinite(location.x())&&Double.isFinite(location.y())?Optional.of(location):Optional.empty();
        }catch(RuntimeException malformed){return Optional.empty();}
    }
    public void save(String world,ResearchBookNavigation.Location location) throws IOException {
        if(!Double.isFinite(location.x())||!Double.isFinite(location.y()))return;
        var root=read();var data=new JsonObject();data.addProperty("category",location.category());data.addProperty("x",location.x());data.addProperty("y",location.y());root.add(world,data);
        Files.createDirectories(file.getParent());var temporary=Files.createTempFile(file.getParent(),"book-location-",".tmp");
        try {Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(root));Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        finally {Files.deleteIfExists(temporary);}
    }
}
