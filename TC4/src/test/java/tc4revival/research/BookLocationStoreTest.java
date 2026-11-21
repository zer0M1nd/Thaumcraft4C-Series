package tc4revival.research;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class BookLocationStoreTest {
    @TempDir Path directory;
    @Test void persistsAcrossOpeningsAndSeparatesPlayersAndWorlds() throws Exception {
        var file=directory.resolve("locations.json");var store=new BookLocationStore(file);var location=new ResearchBookNavigation.Location("ARTIFICE",12.5,-18.25);
        store.save("server/player1",location);store.save("server/player2",new ResearchBookNavigation.Location("BASICS",0,0));
        assertEquals(location,new BookLocationStore(file).load("server/player1").orElseThrow());assertTrue(store.load("different-world/player1").isEmpty());
        Files.writeString(file,"broken");assertTrue(store.load("server/player1").isEmpty());store.save("server/player1",location);assertEquals(location,store.load("server/player1").orElseThrow());
    }
}
