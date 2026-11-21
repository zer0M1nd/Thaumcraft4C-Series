package thaumicomniverse;
/** Generated exact legacy ownership, without optional integration class loading. */
public final class FeatureOwnership {
    private static final com.google.gson.JsonObject DATA;
    static {try(var in=FeatureOwnership.class.getResourceAsStream("/thaumicomniverse/migrated-ownership.json")){if(in==null)throw new IllegalStateException("Missing ownership data");DATA=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}}
    public static String group(String kind,String key){var row=DATA.getAsJsonObject(kind).get(key);return row==null?null:row.getAsString();}
    public static boolean migratedItem(String key){return group("items",key)!=null;}
    public static boolean migratedResearch(String key){return group("research",key)!=null;}
    public static boolean migratedRecipe(String key){return group("recipes",key)!=null;}
}
