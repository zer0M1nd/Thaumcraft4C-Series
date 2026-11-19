package tc4revival.aspect.item;

import com.google.gson.JsonParser;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.editor.EditorDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class TolerantAspectDefinitionsTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void optionalModGatesDoNotAssignVanillaReplacementsWhenTargetIsAbsent() {
        var item=JsonParser.parseString("""
            {"format":1,"required_any_mods":["targetmod","alternate"],"entries":[
              {"items":["minecraft:copper_ingot"],"required_mods":["targetmod"],"nbt":{"kind":"refined"},"aspects":[{"aspect":"metallum","amount":9}]}]}
            """);
        assertTrue(ItemAspectJson.decodeKnown(item,catalog,id->true,mod->false).isEmpty());
        assertTrue(ItemAspectJson.decodeKnown(item,catalog,id->true,mod->mod.equals("alternate")).isEmpty());
        assertEquals(1,ItemAspectJson.decodeKnown(item,catalog,id->true,mod->mod.equals("targetmod")).size());
        var block=JsonParser.parseString("""
            {"format":1,"required_mods":["targetmod"],"entries":[
              {"block":"minecraft:copper_ore","aspects":{"metallum":6}},
              {"block":"minecraft:stone","required_any_mods":["othermod"],"aspects":{"terra":8}}]}
            """);
        assertTrue(new BlockAspectTable(List.of(block),catalog,id->true,mod->false).rules().isEmpty());
        assertEquals(1,new BlockAspectTable(List.of(block),catalog,id->true,mod->mod.equals("targetmod")).rules().size());
    }
    @Test void absentItemsAndUnknownQuantitiesDoNotDiscardValidMembers() {
        var json=JsonParser.parseString("""
            {"format":1,"entries":[
              {"items":["missing:removed","minecraft:stone"],"aspects":[{"aspect":"terra","amount":3},{"aspect":"missing_aspect","amount":5}]},
              {"items":["missing:other"],"aspects":[{"aspect":"aer","amount":9}]},
              {"items":["minecraft:glass"],"aspects":[]}
            ]}
            """);
        var rules=ItemAspectJson.decodeKnown(json,catalog,id->id.startsWith("minecraft:"));
        var resolver=new ItemAspectResolver(rules,catalog);
        assertEquals(2,resolver.itemCount());assertFalse(resolver.contains("missing:removed"));
        assertEquals(Map.of(catalog.require("terra"),3),resolver.resolve("minecraft:stone",tag->true).orElseThrow().view());
        assertTrue(resolver.resolve("minecraft:glass",tag->true).orElseThrow().isEmpty());
        assertEquals(1,resolver.selectedRule("minecraft:stone",tag->true).orElseThrow().aspects().size());
        // Direct resolver and additive equipment callers receive the same per-quantity behavior.
        assertDoesNotThrow(()->new ItemAspectResolver(ItemAspectJson.decode(json),catalog,true).resolve("minecraft:stone",tag->true));
    }
    @Test void unknownBlocksAndAspectsAreSkippedIndependently() {
        var json=JsonParser.parseString("""
            {"format":1,"entries":[
              {"block":"missing:portal","aspects":{"missing_aspect":2}},
              {"block":"minecraft:end_portal","aspects":{"alienis":4,"missing_aspect":9}}
            ]}
            """);
        var table=new BlockAspectTable(List.of(json),catalog,id->id.equals("minecraft:end_portal"));
        assertEquals(1,table.rules().size());assertTrue(table.resolve("missing:portal",Map.of()).isEmpty());
        assertEquals(Map.of(catalog.require("alienis"),4),table.resolve("minecraft:end_portal",Map.of()).orElseThrow().view());
    }
    @Test void loadedEditorOverridesKeepOrderAndExplicitEmptyDefinitions() {
        var source=new EditorDefinitions(Map.of("item:missing:item",Map.of("aer",8),
                "block:minecraft:end_portal",Map.of("alienis",6,"missing_aspect",4),
                "item_tag:forge:stone",Map.of()),17);
        var filtered=source.filtered(catalog,(kind,id)->!id.startsWith("missing:"));
        assertEquals(17,filtered.order());assertEquals(2,filtered.entries().size());
        assertEquals(Map.of("alienis",6),filtered.get("block:minecraft:end_portal").orElseThrow());
        assertTrue(filtered.get("item_tag:forge:stone").orElseThrow().isEmpty());
        assertDoesNotThrow(()->filtered.validateAspects(catalog));
    }
}
