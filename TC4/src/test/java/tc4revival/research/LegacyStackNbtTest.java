package tc4revival.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyStackNbtTest {
    @Test void nodeJarListConvertsToTypedCompoundsWithoutLosingAspectOrder() {
        String old = "{nodetype:0,Aspects:[0:{amount:40,key:\"aer\",},1:{amount:40,key:\"ignis\",},],nodeid:\"\",}";
        assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class, () -> TagParser.parseTag(old));
        var tag = LegacyStackNbt.read(old); var aspects = tag.getList("Aspects", Tag.TAG_COMPOUND);
        assertEquals(2, aspects.size()); assertEquals("aer", aspects.getCompound(0).getString("key"));
        assertEquals("ignis", aspects.getCompound(1).getString("key")); assertEquals(40, aspects.getCompound(1).getInt("amount"));
        assertTrue(tag.contains("nodetype", Tag.TAG_INT)); assertEquals("", tag.getString("nodeid"));
    }
    @Test void nestedListsAndNumericCompoundKeysAreNotConfused() {
        var tag = LegacyStackNbt.read("{0:4,outer:[0:[0:3s,1:4s,],1:[0:5s,],],byte:1b,long:2L,}");
        assertEquals(4, tag.getInt("0")); assertTrue(tag.contains("byte", Tag.TAG_BYTE)); assertTrue(tag.contains("long", Tag.TAG_LONG));
        var outer = tag.getList("outer", Tag.TAG_LIST);
        assertEquals(2, outer.size()); assertEquals(2, outer.getList(0).size()); assertEquals(5, ((net.minecraft.nbt.NumericTag) outer.getList(1).get(0)).getAsShort());
    }
    @Test void stringContentsAndEscapedQuotesRemainByteForByteIntact() throws Exception {
        String source = "{text:\"[0:literal], {1:key}, \\\"quoted\\\"\",list:[0:\"0:one\",1:\"[1:two]\",],single:'[0:three]'}";
        var tag = LegacyStackNbt.read(source);
        assertEquals("[0:literal], {1:key}, \"quoted\"", tag.getString("text"));
        assertEquals("[1:two]", tag.getList("list", Tag.TAG_STRING).getString(1)); assertEquals("[0:three]", tag.getString("single"));
        String modern = "{list:[{x:1},{x:2}],bytes:[B;1b,-2b],ints:[I;1,2],longs:[L;3L,4L]}";
        assertEquals(modern, LegacyStackNbt.normalize(modern)); assertEquals(TagParser.parseTag(modern), LegacyStackNbt.read(modern));
    }
    @Test void malformedListsFailInsteadOfSilentlyChangingTheirMeaning() {
        for (String source : List.of("{x:[1:2]}", "{x:[0:2,2:3]}", "{x:[0:2,3]}", "{x:[2,1:3]}",
                "{x:[0:{a:1]}}", "{x:[999999999999999999:3]}", "{x:[0:1,1:\"two\"]}", "{x:[0:1]"))
            assertThrows(IllegalArgumentException.class, () -> LegacyStackNbt.read(source), source);
    }
    @Test void allObservedBookStackTagsParseAndDoNotShareMutableResults() throws Exception {
        var values = new ArrayList<String>();
        for (String file : List.of("research-catalog.json", "research-pages.json", "research-page-recipes.json")) {
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/" + file), StandardCharsets.UTF_8)) {
                collect(JsonParser.parseReader(reader), values);
            }
        }
        assertEquals(136, values.size()); assertEquals(124, values.stream().distinct().count());
        for (String value : values) assertNotNull(LegacyStackNbt.read(value), value);
        var stack = new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key("Thaumcraft:WandCasting", 0),
                1, false, Optional.of("{cap:\"iron\",rod:\"wood\",}"), Optional.empty());
        stack.tag().orElseThrow().putString("cap", "gold");
        assertEquals("iron", stack.tag().orElseThrow().getString("cap"));
        assertNull(ResearchClues.readNbt(Optional.empty()));
    }
    private void collect(JsonElement element, List<String> values) {
        if (element.isJsonArray()) element.getAsJsonArray().forEach(value -> collect(value, values));
        else if (element.isJsonObject()) element.getAsJsonObject().entrySet().forEach(entry -> {
            if (entry.getKey().equals("nbt") && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString())
                values.add(entry.getValue().getAsString());
            collect(entry.getValue(), values);
        });
    }
}
