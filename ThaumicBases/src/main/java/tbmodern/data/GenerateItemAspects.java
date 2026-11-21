package tbmodern.data;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import tc4revival.aspect.AspectDefinitions;
import tc4revival.aspect.recipe.*;

/** Build-time data generation using the dependency's existing calculation engine, without launching Minecraft. */
public final class GenerateItemAspects {
    public static void main(String[] args) throws Exception {
        if(Files.exists(Path.of("src/main/resources/data/thaumicbases/item_aspects/explicit.json")))
            throw new IllegalStateException("Historical generator retired: edit the complete explicit aspect definitions instead");
        var root=JsonParser.parseString(Files.readString(Path.of(".tooling/aspect-generation-input.json"))).getAsJsonObject();
        var catalog=AspectDefinitions.decode(root.getAsJsonArray("catalog"));
        var data=RecipeAspectJson.decode(root.getAsJsonObject("graph"));
        Set<String> explicit=new HashSet<>();root.getAsJsonArray("explicit").forEach(value->explicit.add(value.getAsString()));
        var rows=new JsonArray();var report=new JsonObject();var brightVariants=new JsonObject();
        for(var value:root.getAsJsonArray("targets")) {
            String id=value.getAsString();if(explicit.contains(id))continue;
            // Independent sessions avoid generation order becoming a persistent gameplay input.
            var amounts=new RecipeAspectEngine(data,catalog).object(new RecipeAspectData.Key(id,0));
            var quantities=new JsonArray();
            if(amounts!=null)amounts.view().forEach((aspect,amount)->{var row=new JsonObject();row.addProperty("aspect",aspect.tag());row.addProperty("amount",amount);quantities.add(row);});
            var row=new JsonObject();var items=new JsonArray();items.add(id);row.add("items",items);row.add("aspects",quantities);row.addProperty("generated",true);
            if(id.equals("thaumicbases:node_focus_brightness")) {
                brightVariants.add("pearl",row);
                var alternate=root.getAsJsonObject("graph").deepCopy();var recipes=alternate.getAsJsonArray("recipes");
                for(int i=0;i<recipes.size();i++)if(recipes.get(i).getAsJsonObject().getAsJsonObject("output").get("item").getAsString().equals(id))recipes.set(i,root.getAsJsonObject("bright_fallback"));
                var alternateAmounts=new RecipeAspectEngine(RecipeAspectJson.decode(alternate),catalog).object(new RecipeAspectData.Key(id,0));
                if(alternateAmounts==null)throw new IllegalStateException("Missing fallback brightness aspects");
                var alternative=row.deepCopy();var alternativeValues=new JsonArray();
                alternateAmounts.view().forEach((aspect,amount)->{var q=new JsonObject();q.addProperty("aspect",aspect.tag());q.addProperty("amount",amount);alternativeValues.add(q);});
                alternative.add("aspects",alternativeValues);brightVariants.add("charm",alternative);
            } else rows.add(row);
            report.addProperty(id,amounts==null?"no legacy recipe value":"generated");
        }
        var output=new JsonObject();output.addProperty("format",1);output.add("entries",rows);
        var gson=new GsonBuilder().setPrettyPrinting().create();
        Path path=Path.of("src/main/resources/data/thaumicbases/item_aspects/derived.json");Files.createDirectories(path.getParent());
        Files.writeString(path,gson.toJson(output)+"\n",StandardCharsets.UTF_8);
        Files.writeString(Path.of("src/main/resources/tbmodern/bright-aspects.json"),gson.toJson(brightVariants)+"\n",StandardCharsets.UTF_8);
        Files.writeString(Path.of("docs/aspect-generation.json"),gson.toJson(report)+"\n",StandardCharsets.UTF_8);
        System.out.println("Generated "+rows.size()+" addon item aspect rules");
    }
    private GenerateItemAspects() {}
}
