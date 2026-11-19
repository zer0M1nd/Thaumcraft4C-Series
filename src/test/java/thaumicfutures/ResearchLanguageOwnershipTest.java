package thaumicfutures;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResearchLanguageOwnershipTest {
 @Test void dynamicDisplayFamiliesShipWithoutOmniverse()throws Exception{
  var keys=new TreeSet<String>();keys.add("thaumicfutures.sky.sun");
  for(var direction:List.of("north","east","south","west"))keys.add("thaumicfutures.sky.stars_"+direction);
  for(int i=0;i<8;i++){keys.add("thaumicfutures.sky.moon_"+i);keys.add("omniverse.bees.moon."+i);}
  for(var face:List.of("down","up","north","south","west","east"))keys.add("omniverse.machine.face."+face);
  keys.add("material.thaumicfutures.thaumium");keys.add("modifier.thaumicfutures.thaumic");
  keys.add("modifier.thaumicfutures.thaumic.flavor");keys.add("modifier.thaumicfutures.thaumic.description");
  for(int i=0;i<4;i++)keys.add("thaumicfutures.autocaster.target."+i);
  for(var lang:List.of("zh_cn","en_us")){
   var data=json(Path.of("src/main/resources/assets/thaumicfutures/lang/"+lang+".json"));
   for(var key:keys){assertTrue(data.has(key),lang+" missing "+key);assertFalse(data.get(key).getAsString().isBlank(),key);}
   assertEquals(lang.equals("zh_cn")?"太阳":"Sun",data.get("thaumicfutures.sky.sun").getAsString());
  }
 }
 @Test void allOwnedResearchTranslationsShipWithoutOmniverse()throws Exception{
  var root=Path.of("src/main/resources");var keys=new TreeSet<String>();
  try(var files=Files.list(root.resolve("data/thaumicfutures/research_catalog"))){for(var f:files.toList())for(var cat:json(f).getAsJsonArray("categories"))for(var row:cat.getAsJsonObject().getAsJsonArray("research"))for(var prefix:List.of("tc.research_name.","tc.research_text."))keys.add(prefix+row.getAsJsonObject().get("key").getAsString());}
  try(var files=Files.list(root.resolve("data/thaumicfutures/research_pages"))){for(var f:files.toList())for(var entry:json(f).getAsJsonArray("entries"))for(var page:entry.getAsJsonObject().getAsJsonArray("pages"))if(page.getAsJsonObject().has("text"))keys.add(page.getAsJsonObject().getAsJsonObject("text").get("translate").getAsString());}
  for(var lang:List.of("zh_cn","en_us")){var data=json(root.resolve("assets/thaumicfutures/lang/"+lang+".json"));for(var key:keys){assertTrue(data.has(key),lang+" missing "+key);assertFalse(data.get(key).getAsString().isBlank(),key);}}
 }
 static JsonObject json(Path path)throws Exception{return JsonParser.parseString(Files.readString(path)).getAsJsonObject();}
}
