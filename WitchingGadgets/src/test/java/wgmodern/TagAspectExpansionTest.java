package wgmodern;
import com.google.gson.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class TagAspectExpansionTest {
 @Test public void clientAndReloadMembershipProduceIdenticalStableRules(){
  var raw=JsonParser.parseString("{\"format\":1,\"entries\":[],\"witchinggadgets:tag_aspects\":[{\"tag\":\"forge:bones/wither\",\"aspects\":[{\"aspect\":\"mortuus\",\"amount\":4}]}]}");
  var server=raw.deepCopy();var client=raw.deepCopy();
  TagAspectExpansion.expand(server,id->List.of("example:b","example:a","example:a"));
  TagAspectExpansion.expand(client,id->List.of("example:a","example:b"));
  assertEquals(server,client);assertEquals(1,client.getAsJsonObject().getAsJsonArray("entries").size());
  TagAspectExpansion.expand(client,id->{throw new AssertionError("Already expanded");});assertEquals(server,client);
  var missing=raw.deepCopy();TagAspectExpansion.expand(missing,id->List.of());assertTrue(missing.getAsJsonObject().getAsJsonArray("entries").isEmpty());
  assertTrue(raw.getAsJsonObject().has("witchinggadgets:tag_aspects"));
 }
}
