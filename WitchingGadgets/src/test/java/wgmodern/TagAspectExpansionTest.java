package wgmodern;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class TagAspectExpansionTest {
    @Test void aPendingReloadKeepsTheLastReadySnapshotAvailable() {
        var ready = new AtomicBoolean();
        var requests = new AtomicInteger();
        Supplier<Map<ResourceLocation, Collection<Holder<Item>>>> pending = () -> {
            requests.incrementAndGet();
            if (!ready.get()) throw new IllegalStateException("Tags have not been loaded yet.");
            return Map.of();
        };
        try {
            ready.set(true);
            TagAspectExpansion.loadItemTags(pending).onResourceManagerReload(null);
            ready.set(false);
            requests.set(0);
            var next = TagAspectExpansion.loadItemTags(pending);
            var input = JsonParser.parseString("""
                    {"entries":[],"witchinggadgets:tag_aspects":[{"tag":"forge:ingots/iron","aspects":[]}]}
                    """);
            TagAspectExpansion.expand(input);
            assertEquals(0, requests.get());
            ready.set(true);
            next.onResourceManagerReload(null);
            assertEquals(1, requests.get());
        } finally {
            TagAspectExpansion.stopped(null);
        }
    }
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
