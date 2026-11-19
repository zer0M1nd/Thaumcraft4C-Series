package tc4revival.aspect.editor;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
class EditorDefinitionsTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @TempDir Path packs;
    @Test void emptyDefinitionSurvivesRoundTripAndLaterSaveWinsIndependentOfInputOrder(){
        var first=new EditorDefinitions(Map.of("item:minecraft:ender_pearl",Map.of("aer",17)),1);
        var next=new EditorDefinitions(Map.of("item:minecraft:ender_pearl",Map.of()),2);
        var merged=EditorDefinitions.merge(List.of(next.encode(),first.encode()));
        assertTrue(merged.get("item:minecraft:ender_pearl").isPresent());assertTrue(merged.get("item:minecraft:ender_pearl").get().isEmpty());
        assertTrue(merged.get("item:minecraft:stone").isEmpty());
    }
    @Test void directDefinitionsWinTagsWhileEditedTagsOverrideRecipeFallbacks(){
        var direct=new ItemAspectRule(List.of("minecraft:oak_log"),null,List.of(new ItemAspectRule.Quantity("arbor",4)));
        var generated=new ItemAspectRule(List.of("minecraft:cherry_log"),null,List.of(new ItemAspectRule.Quantity("arbor",4)),true);
        var defs=new EditorDefinitions(Map.of("item_tag:minecraft:logs",Map.of("aer",127)),1);
        var resolver=new EditorItemResolver(new ItemAspectResolver(List.of(direct,generated),catalog),defs,catalog,(id,tag)->true);
        assertEquals(4,resolver.resolve("minecraft:oak_log",tag->true).orElseThrow().amount(catalog.require("arbor")));
        assertEquals(127,resolver.resolve("minecraft:cherry_log",tag->true).orElseThrow().amount(catalog.require("aer")));
        assertTrue(resolver.resolve("minecraft:unmatched",tag->false).isEmpty());
        var cleared=new EditorItemResolver(resolver.original(),new EditorDefinitions(Map.of("item:minecraft:oak_log",Map.of()),2),catalog,(id,tag)->true);
        assertTrue(cleared.resolve("minecraft:oak_log",tag->true).orElseThrow().isEmpty());
    }
    @Test void itemAndBlockTagsCannotCollideAndMostRecentlyEditedMatchingTagWins(){
        var a=new EditorDefinitions(Map.of("item_tag:test:one",Map.of("aer",2),"block_tag:test:one",Map.of("terra",3)),1);
        var b=new EditorDefinitions(Map.of("item_tag:test:two",Map.of("aer",4)),2);
        var c=new EditorDefinitions(Map.of("item_tag:test:one",Map.of()),3);
        var merged=EditorDefinitions.merge(List.of(a.encode(),c.encode(),b.encode()));
        assertTrue(merged.tag("item",t->true).orElseThrow().isEmpty());
        assertEquals(Map.of("terra",3),merged.tag("block",t->true).orElseThrow());
    }
    @Test void tagEditingCanInspectTagRuleEvenWhenItemHasADirectRule(){
        var tagged=new ItemAspectRule(List.of("minecraft:oak_log"),"minecraft:logs",List.of(new ItemAspectRule.Quantity("arbor",3)));
        var direct=new ItemAspectRule(List.of("minecraft:oak_log"),null,List.of(new ItemAspectRule.Quantity("arbor",4)));
        var resolver=new EditorItemResolver(new ItemAspectResolver(List.of(tagged,direct),catalog),EditorDefinitions.EMPTY,catalog,(id,tag)->true);
        assertEquals(direct,resolver.selectedRule("minecraft:oak_log",t->true).orElseThrow());
        assertEquals(tagged,resolver.tagRule("minecraft:oak_log","minecraft:logs").orElseThrow());
        assertTrue(resolver.tagRule("minecraft:oak_log","minecraft:planks").isEmpty());
    }
    @Test void savesAreAppendOnlyMonotonicAndPlayerNamesIncrement()throws Exception{
        var edit=new EditorDefinitions(Map.of("block:minecraft:end_portal",Map.of("aer",10)),0);
        assertEquals("Admin_1",EditorPackStore.nextName(packs,"Admin"));
        var a=EditorPackStore.write(packs,"","Admin",edit,0);
        assertEquals("Admin_2",EditorPackStore.nextName(packs,"Admin"));
        var b=EditorPackStore.write(packs,"中文名称","Admin",edit,0);
        var c=EditorPackStore.write(packs,"中文名称","Admin",edit,0);
        assertEquals(a.order()+1,b.order());assertEquals(b.order()+1,c.order());
        assertNotEquals(b.directory(),c.directory());assertTrue(Files.exists(a.directory().resolve("pack.mcmeta")));
        assertThrows(IllegalArgumentException.class,()->EditorPackStore.write(packs,"../escape","Admin",edit,0));
        assertEquals(3,Files.list(packs).count());
    }
    @Test void invalidAndFractionalQuantitiesAndUnknownAspectsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new EditorDefinitions(Map.of("item:minecraft:stone",Map.of("aer",10001)),0));
        assertThrows(IllegalArgumentException.class,()->new EditorDefinitions(Map.of("fluid:minecraft:water",Map.of()),0));
        assertThrows(IllegalArgumentException.class,()->new EditorDefinitions(Map.of("item:minecraft:stone",Map.of("unknown",1)),0).validateAspects(catalog));
        var json=new EditorDefinitions(Map.of("item:minecraft:stone",Map.of("aer",1)),0).encode();
        json.getAsJsonObject("entries").getAsJsonObject("item:minecraft:stone").addProperty("aer",1.5);
        assertThrows(ArithmeticException.class,()->EditorDefinitions.decode(json));
    }
    @Test void tiersGroupedRowsAndNumericAliasesSupportMoreThanOnePage(){
        assertEquals(0,EditorAspectLayout.tier(catalog.require("aer"),catalog));
        assertEquals(1,EditorAspectLayout.tier(catalog.require("victus"),catalog));
        for(int mode=0;mode<6;mode++){
            var rows=EditorAspectLayout.rows(catalog,mode,3);
            assertTrue(rows.size()>10);assertEquals(catalog.all().size(),rows.stream().mapToInt(List::size).sum());
            for(var row:rows){assertTrue(row.size()<=3);if(mode>=4)assertEquals(1,row.stream().map(a->EditorAspectLayout.tier(a,catalog)).distinct().count());}
        }
        assertEquals(List.of(4,5,6,7,8,9,0),"QWEASDZ".chars().map(EditorAspectLayout::digit).boxed().toList());
        assertEquals(123,EditorAspectLayout.append(EditorAspectLayout.append(1,2),3));
        assertEquals(10000,EditorAspectLayout.append(9999,9));
    }
    @Test void uploadsRoundTripMultipleFramesAndRejectOrderReplayAndOversize(){
        String json="中文".repeat(30000);var frames=EditorUpload.frames(json);assertTrue(frames.size()>1);
        var receiver=new EditorUpload.Receiver();String result=null;
        for(int i=0;i<frames.size();i++)result=receiver.accept(i,frames.size(),frames.get(i)).orElse(result);
        assertEquals(json,result);
        assertThrows(IllegalArgumentException.class,()->receiver.accept(0,frames.size(),frames.get(0)));
        assertThrows(IllegalArgumentException.class,()->new EditorUpload.Receiver().accept(1,2,frames.get(0)));
        assertThrows(IllegalArgumentException.class,()->EditorUpload.frames("x".repeat(EditorUpload.LIMIT+1)));
    }
    @Test void entityDefinitionsStayDistinctFromItemsAndPreserveExplicitEmpty(){
        var definitions=new EditorDefinitions(Map.of("entity:minecraft:pig",Map.of("aer",37),"entity:minecraft:cow",Map.of(),"item:minecraft:pig_spawn_egg",Map.of("terra",3)),7);
        var decoded=EditorDefinitions.decode(definitions.encode());
        assertEquals(Map.of("aer",37),decoded.get("entity:minecraft:pig").orElseThrow());
        assertTrue(decoded.get("entity:minecraft:cow").orElseThrow().isEmpty());
        assertEquals(Map.of("terra",3),decoded.get("item:minecraft:pig_spawn_egg").orElseThrow());
    }
    @Test void technicalBlocksAndEntitiesMatchLocalizedNamesIdsAndNamespaces(){
        assertTrue(EditorNavigation.matchesNonItem("末地传送门","block:minecraft:end_portal","末地传送门"));
        assertTrue(EditorNavigation.matchesNonItem("end_portal","block:minecraft:end_portal","末地传送门"));
        assertTrue(EditorNavigation.matchesNonItem("@minecraft 末地","block:minecraft:end_portal","末地传送门"));
        assertTrue(EditorNavigation.matchesNonItem("ENTITY:minecraft:pig","entity:minecraft:pig","猪"));
        assertTrue(EditorNavigation.matchesNonItem("猪","entity:minecraft:pig","猪"));
        assertFalse(EditorNavigation.matchesNonItem("@ae2 末地","block:minecraft:end_portal","末地传送门"));
        assertFalse(EditorNavigation.matchesNonItem("僵尸","entity:minecraft:pig","猪"));
    }
    @Test void scrollbarDragReachesBothEndsClampsAndKeepsGrabOffset(){
        int thumb=EditorNavigation.thumbHeight(19,981,434);
        assertEquals(10,thumb);
        assertEquals(0,EditorNavigation.dragRow(110,5,105,434,thumb,981));
        assertEquals(981,EditorNavigation.dragRow(534,5,105,434,thumb,981));
        assertEquals(491,EditorNavigation.dragRow(322,5,105,434,thumb,981));
        assertEquals(981,EditorNavigation.dragRow(10000,5,105,434,thumb,981));
        assertEquals(0,EditorNavigation.dragRow(-100,5,105,434,thumb,981));
        assertEquals(434,EditorNavigation.thumbHeight(19,0,434));
        assertEquals(0,EditorNavigation.dragRow(500,0,105,434,434,0));
    }
    @Test void entityPreviewUsesSynchronizedBaseAndContributionWithoutSpawning(){
        var rule=new tc4revival.aspect.entity.EntityAspectRule("minecraft:pig","Pig",tc4revival.aspect.entity.EntityAspectRule.Variant.ANY,"",List.of(new tc4revival.aspect.entity.EntityAspectRule.Quantity("bestia",2)));
        String json=tc4revival.aspect.entity.EntityAspectJson.encode(List.of(rule)).toString();
        String extra="{\"format\":1,\"entries\":[{\"entity\":\"minecraft:pig\",\"operation\":\"add\",\"aspects\":{\"aer\":3}}]}";
        var defaults=new EditorEntityDefaults(new tc4revival.aspect.sync.AspectDataSet(Map.of("entity_aspects/test:base",json,"entity_aspect_contributions/test:extra",extra)),catalog,id->true);
        assertEquals(Map.of("bestia",2,"aer",3),EditorDefinitions.values(defaults.preview("minecraft:pig").orElseThrow()));
        assertTrue(defaults.preview("test:unknown").isEmpty());
    }
}
