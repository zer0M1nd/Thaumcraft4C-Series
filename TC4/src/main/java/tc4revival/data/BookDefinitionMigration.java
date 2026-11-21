package tc4revival.data;

import com.google.gson.*;

/** Import preserved captures and legacy addon pages while retaining explicit modern components. */
public final class BookDefinitionMigration {
    private BookDefinitionMigration() {}
    public static void upgradeMissingIds(JsonArray entries,String namespace) {
        for(var value:entries) {
            var entry=value.getAsJsonObject();String key=entry.get("key").getAsString();int index=0;
            for(var element:entry.getAsJsonArray("pages")) {
                var page=element.getAsJsonObject();index++;
                if(page.has("id"))continue;
                page.addProperty("id",(key.contains(":")?key:namespace+":"+key)+"/page/"+index);
                if(!page.has("page_break"))page.addProperty("page_break",true);
                if(page.has("text")&&page.get("text").isJsonPrimitive()) {
                    page.add("text",tc4revival.research.ResearchPage.translation(page.get("text").getAsString()));
                    page.addProperty("import_legacy",true);
                }
            }
        }
    }
    public static void upgrade(JsonArray entries,String namespace) {
        for(var value:entries) {
            var entry=value.getAsJsonObject();String key=entry.get("key").getAsString();int index=0;
            for(var element:entry.getAsJsonArray("pages")) {
                var page=element.getAsJsonObject();index++;
                if(!page.has("id"))page.addProperty("id",(key.contains(":")?key:namespace+":"+key)+"/page/"+index);
                if(!page.has("page_break"))page.addProperty("page_break",true);
                if(page.has("text")&&page.get("text").isJsonPrimitive()) {
                    page.add("text",tc4revival.research.ResearchPage.translation(page.get("text").getAsString()));
                    page.addProperty("import_legacy",true);
                }
            }
        }
    }
}
