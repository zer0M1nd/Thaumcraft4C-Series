package tc4revival.research;

/** Old original captures remain immutable; tests explicitly import them into the new contract. */
public final class BookTestData {
    public static ResearchPages pages(com.google.gson.JsonArray source) {
        var copy=source.deepCopy();tc4revival.data.BookDefinitionMigration.upgrade(copy,"thaumcraft");return ResearchPages.decode(copy);
    }
    public static ResearchPage page(com.google.gson.JsonObject source) {
        var copy=source.deepCopy();if(!copy.has("id"))copy.addProperty("id","test:component");
        return ResearchPage.decode(copy);
    }
}
