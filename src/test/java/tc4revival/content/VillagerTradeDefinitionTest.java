package tc4revival.content;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VillagerTradeDefinitionTest {
    private String json(String extra) { return "{\"format\":1,\"levels\":{\"1\":[{\"buy\":{\"item\":\"minecraft:emerald\",\"count\":2},\"sell\":{\"items\":[\"minecraft:book\",\"minecraft:paper\"],\"min\":1,\"max\":3}"+extra+"}]}}"; }
    private VillagerTradeDefinition decode(String text) { return VillagerTradeDefinition.decode(JsonParser.parseString(text).getAsJsonObject()); }
    @Test void optionalSecondInputAndNbtAndAllFiveLevels() {
        var def=decode(json(",\"buy_b\":{\"item\":\"minecraft:book\",\"nbt\":\"{CustomModelData:2}\"},\"max_uses\":16,\"xp\":10,\"price_multiplier\":0.2,\"reward_exp\":false"));
        var t=def.levels().get(1).get(0);
        assertEquals(5,def.levels().size());assertTrue(def.levels().get(5).isEmpty());
        assertEquals(2,t.buy().min());assertEquals(3,t.sell().max());assertEquals(2,t.sell().items().size());
        assertEquals("{CustomModelData:2}",t.buyB().nbt());assertEquals(10,t.xp());assertEquals(16,t.maxUses());assertFalse(t.rewardExp());
    }
    @Test void rejectInvalidRangesAndIdentifiers() {
        for(String extra:new String[]{",\"max_uses\":0",",\"xp\":-1",",\"price_multiplier\":-0.1"})assertThrows(IllegalArgumentException.class,()->decode(json(extra)));
        assertThrows(IllegalArgumentException.class,()->decode(json("").replace("minecraft:book","Minecraft:Book")));
        assertThrows(IllegalArgumentException.class,()->decode(json("").replace("\"max\":3","\"max\":0")));
        assertThrows(IllegalArgumentException.class,()->decode(json("").replace("\"1\":[","\"6\":[")));
    }
    @Test void bundledCatalogKeepsAllCandidatesAndProvidesEveryLevel() throws Exception {
        for(String role:new String[]{"wizard","banker"})try(var in=getClass().getResourceAsStream("/data/thaumcraft/villager_trades/"+role+".json")) {
            assertNotNull(in);var def=decode(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(role.equals("wizard")?15:12,def.levels().values().stream().mapToInt(java.util.List::size).sum());
            for(var rows:def.levels().values()) { assertTrue(rows.size()>=2);assertTrue(rows.stream().allMatch(t->t.xp()>0&&t.multiplier()>0)); }
        }
    }
}
