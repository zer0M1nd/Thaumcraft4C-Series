package conciliummodern;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchResourceTest {
    static final Path ROOT=Path.of("src/main/resources");
    static JsonObject read(String path)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
    @Test void currentBookComponentsHaveUniqueIdsAndExactCatalogCounts()throws Exception{
        var pages=read("data/thaumicconcilium/research_pages/main.json").getAsJsonArray("entries");
        var counts=new HashMap<String,Integer>();var ids=new HashSet<String>();
        for(var row:pages){var entry=row.getAsJsonObject();String key=entry.get("key").getAsString();assertNull(counts.put(key,entry.getAsJsonArray("pages").size()),key);
            for(var value:entry.getAsJsonArray("pages")){var page=value.getAsJsonObject();assertTrue(page.has("id"),key);assertTrue(ids.add(page.get("id").getAsString()),key);
                assertTrue(page.get("page_break").getAsBoolean(),key);
                if(page.has("text"))assertTrue(page.get("text").isJsonObject(),key);
                assertNotNull(tc4revival.research.ResearchPage.decode(page));
            }
        }
        for(var category:read("data/thaumicconcilium/research_catalog/main.json").getAsJsonArray("categories"))
            for(var value:category.getAsJsonObject().getAsJsonArray("research")){var row=value.getAsJsonObject();String key=row.get("key").getAsString();
                if(!row.has("mirrorOf"))assertEquals(row.get("pageCount").getAsInt(),counts.get(key),key);
            }
    }
    @Test void everyActiveOriginalResearchIsRepresented()throws Exception{
        String original=Files.readString(Path.of("../../../References/Projects/ThaumicConcilium/upstream/src/main/java/com/ilya3point999k/thaumicconcilium/common/registry/Thaumonomicon.java"));
        original=original.replaceAll("(?s)/\\*.*?\\*/","").replaceAll("//[^\\r\\n]*","");
        var expected=new HashSet<String>();var matcher=Pattern.compile("new ResearchItem\\(\"([^\"]+)\"").matcher(original);
        while(matcher.find())expected.add("thaumicconcilium:"+matcher.group(1));
        var actual=new HashSet<String>();for(var cat:read("data/thaumicconcilium/research_catalog/main.json").getAsJsonArray("categories"))
            for(var row:cat.getAsJsonObject().getAsJsonArray("research"))actual.add(row.getAsJsonObject().get("key").getAsString());
        expected.removeAll(actual);assertEquals(Set.of(),expected,"Missing active original research");
    }
    @Test void moduleParentsAndConditionalCreaturePagesDoNotReferenceMissingModuleResearch()throws Exception{
        var rows=read("data/thaumicconcilium/research_catalog/main.json").getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research");
        var keys=new HashSet<String>();rows.forEach(row->keys.add(row.getAsJsonObject().get("key").getAsString()));
        for(var value:rows){var row=value.getAsJsonObject();for(String field:List.of("parents","parentsHidden","siblings"))if(row.has(field))for(var parent:row.getAsJsonArray(field)){
            String key=parent.getAsString();if(key.startsWith("thaumicconcilium:"))assertTrue(keys.contains(key),row.get("key")+" -> "+key);
            assertFalse(Set.of("FOCUS_HEAL","FOCUS_XP_DRAIN","CREATION").contains(key),key);
        }}
        var entry=read("data/thaumicconcilium/research_pages/main.json").getAsJsonArray("entries").asList().stream().map(JsonElement::getAsJsonObject).filter(e->e.get("key").getAsString().equals("thaumicconcilium:THAUMICCONCILIUM")).findFirst().orElseThrow();
        assertEquals(26,entry.getAsJsonArray("pages").size());int conditional=0;
        for(var p:entry.getAsJsonArray("pages")){var page=p.getAsJsonObject();if(page.has("research")){conditional++;assertEquals("TEXT_CONCEALED",page.get("type").getAsString());assertTrue(keys.contains(page.get("research").getAsString()));}}
        assertEquals(18,conditional);
    }
    @Test void itemModelResourceLocationsAreValidEvenForDormantItems()throws Exception{
        try(var paths=Files.walk(ROOT.resolve("assets/thaumicconcilium/models"))){
            for(var path:paths.filter(p->p.toString().endsWith(".json")).toList()){
                var model=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if(model.has("parent"))assertTrue(model.get("parent").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"),path.toString());
                if(model.has("textures"))for(var texture:model.getAsJsonObject("textures").entrySet()){
                    var id=texture.getValue().getAsString();assertTrue(id.startsWith("#")||id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"),path+" -> "+id);
                }
            }
        }
    }
}
