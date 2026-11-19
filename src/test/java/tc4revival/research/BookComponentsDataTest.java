package tc4revival.research;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookComponentsDataTest {
    private JsonObject source(){return JsonParser.parseString("""
        {"categories":[{"research":[{"key":"R","pageCount":1}]}],"pages":[{"key":"R","pages":[
          {"id":"test:original","type":"TEXT","text":{"text":"original"},"page_break":true}]}],"recipes":[]}
        """).getAsJsonObject();}
    @Test void additionsHaveStableOrderingAndPreserveSourceAndMixedFlow(){
        var original=source();original.add("additions",JsonParser.parseString("""
          [{"id":"test:z","research":"R","position":"start","priority":2,"components":[{"id":"test:second","type":"TEXT","text":"second"}]},
           {"id":"test:a","research":"R","position":"start","priority":1,"components":[{"id":"test:first","type":"TEXT","text":"first"}]},
           {"id":"test:last","research":"R","position":"end","priority":0,"components":[{"id":"test:last","type":"ITEM","item":"minecraft:stone"}]}]
          """));
        var result=ResearchPageAdditions.apply(original);
        var pages=ResearchPages.decode(result.getAsJsonArray("pages")).require("R");
        assertEquals(java.util.List.of("test:first","test:second","test:original","test:last"),pages.stream().map(ResearchPage::id).toList());
        assertTrue(pages.stream().allMatch(ResearchPage::flow));assertTrue(pages.stream().noneMatch(ResearchPage::pageBreak));
        assertTrue(original.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages").get(0).getAsJsonObject().get("page_break").getAsBoolean());
        assertEquals(4,result.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject().get("pageCount").getAsInt());
    }
    @Test void duplicateAnchorsAreRejectedAcrossDifferentResearch(){
        var json=source();var next=json.getAsJsonArray("pages").get(0).deepCopy().getAsJsonObject();next.addProperty("key","S");json.getAsJsonArray("pages").add(next);
        assertThrows(IllegalArgumentException.class,()->ResearchPages.decode(json.getAsJsonArray("pages")));
    }
    @Test void nestedAndDerivedAnchorsCannotCollideWithAnotherComponent(){
        var json=source();var page=json.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages").get(0).getAsJsonObject();
        page.add("text",JsonParser.parseString("{\"text\":\"x\",\"extra\":[{\"id\":\"test:original\",\"text\":\"bad\"}]}"));
        assertThrows(IllegalArgumentException.class,()->ResearchPages.decode(json.getAsJsonArray("pages")));
        page.add("text",JsonParser.parseString("{\"text\":\"x\",\"extra\":[{\"id\":\"test:anchor\",\"text\":\"ok\"}]}"));
        assertEquals(1,ResearchPages.decode(json.getAsJsonArray("pages")).require("R").size());
    }
    @Test void concealedCanUseConditionsWithoutResearchAndPlayerPredicatesCompose(){
        var json=JsonParser.parseString("""
          [{"key":"R","pages":[{"id":"test:conditional","type":"TEXT_CONCEALED","text":{"text":"conditional"},
          "when":{"all":[{"research":"A"},{"not":{"research":"B"}}]}}]}]
          """).getAsJsonArray();
        var pages=ResearchPages.decode(json);
        assertEquals(1,pages.visible("R","A"::equals,java.util.Map.of()).size());
        assertEquals(0,pages.visible("R",key->true,java.util.Map.of()).size());
        assertEquals(0,pages.visible("R",key->false,java.util.Map.of()).size());
    }
    @Test void configComparisonsHandleNumbersListsAndTypeMismatches(){
        assertTrue(BookConfigCondition.compare(JsonParser.parseString("5"),">=",JsonParser.parseString("5.0")));
        assertTrue(BookConfigCondition.compare(JsonParser.parseString("[\"aer\",\"terra\"]"),"contains",new JsonPrimitive("terra")));
        assertFalse(BookConfigCondition.compare(new JsonPrimitive("5"),"<",new JsonPrimitive(6)));
        assertFalse(BookConfigCondition.compare(new JsonPrimitive(false),"eq",new JsonPrimitive("false")));
    }
    @Test void groupingRetainsOnlyKnownEntriesAndRepackagesFourPerPage(){
        var json=source();var group=JsonParser.parseString("""
          {"id":"test:group","type":"test:renderer","data":{},"group":{"size":4,"field":"values","entries":[]}}
          """).getAsJsonObject();
        for(int i=0;i<7;i++){var row=new JsonObject();row.addProperty("id","value-"+i);row.addProperty("value",i);var when=new JsonObject();when.addProperty("research","R"+i);row.add("when",when);group.getAsJsonObject("group").getAsJsonArray("entries").add(row);}
        json.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages").add(group);
        var pages=ResearchPages.decode(json.getAsJsonArray("pages"));
        var shown=pages.visible("R",key->!key.equals("R2")&&!key.equals("R5"),java.util.Map.of());
        assertEquals(3,shown.size());assertEquals(4,((ResearchPage.Custom)shown.get(1).recipe()).data().getAsJsonArray("values").size());
        assertEquals(6,((ResearchPage.Custom)shown.get(2).recipe()).data().getAsJsonArray("values").get(0).getAsInt());
        assertEquals(1,pages.visible("R",key->false,java.util.Map.of()).size());
    }
}
