package wgmodern;
import java.util.*;import java.util.concurrent.CompletableFuture;import net.minecraft.data.CachedOutput;import static wgmodern.WGData.*;
public class MiscData {
 public static void add(WGData d,CachedOutput c,List<CompletableFuture<?>> jobs){
  for(String food:List.of("sweetwart","nethercake","brainjerky"))jobs.add(d.save(c,"assets/witchinggadgets/models/item/"+food+".json",map("parent","minecraft:item/generated","textures",map("layer0","witchinggadgets:items/food_"+food))));
  jobs.add(d.save(c,"data/witchinggadgets/recipes/sweetwart.json",map("type","minecraft:crafting_shapeless","ingredients",List.of(item("minecraft:nether_wart"),item("minecraft:sugar")),"result",item("witchinggadgets:sweetwart"))));
  jobs.add(d.save(c,"data/witchinggadgets/recipes/nethercake.json",map("type","minecraft:crafting_shaped","pattern",List.of("nnn","www"),"key",map("n",item("witchinggadgets:sweetwart"),"w",item("minecraft:wheat")),"result",item("witchinggadgets:nethercake"))));
  var osmotic=new ArrayList<Object>();String[] enchant={"invisible_gear","unveiling","stealth","backstab","ride_protect"},legacy={"invisgear","unveiling","stealth","backstab","rideprotect"},research={"INVISIBLEGEAR","UNVEILING","STEALTH","BACKSTAB","RIDEPROTECT"};var costs=List.of(Map.of("aer",25,"ordo",20,"perditio",15),Map.of("aer",25,"ordo",20,"aqua",10),Map.of("aer",10,"ordo",20,"terra",10),Map.of("aer",20,"perditio",20,"ignis",20),Map.of("aer",20,"perditio",20,"ordo",20));for(int i=0;i<enchant.length;i++)osmotic.add(map("enchantment","witchinggadgets:"+enchant[i],"research","witchinggadgets:ENCH_"+research[i],"icon","witchinggadgets:textures/gui/research/icon_ench_"+legacy[i]+".png","aspects",costs.get(i)));jobs.add(d.save(c,"data/witchinggadgets/osmotic_enchantments/main.json",map("entries",osmotic)));
  GemAffinities.TAGS.forEach((tag,value)->jobs.add(d.save(c,"data/witchinggadgets/recipes/gem_affinities/"+tag.getPath().replace('/','_')+".json",map("type","witchinggadgets:gem_affinity","ingredient",map("tag",tag.toString()),"affinities",value.affinities(),"aversions",value.aversions(),"priority",1000))));
  var aspects=new ArrayList<Object>();
  aspect(aspects,"title_crimson_cult",Map.of("sensus",2,"humanus",1));
  aspect(aspects,"cutting_table",Map.of("arbor",2,"vitreus",2,"fabrico",2));
  aspect(aspects,"ethereal_wall",Map.of("machina",1,"terra",2));
  aspect(aspects,"rose_vines",Map.of("herba",6,"perditio",4,"praecantatio",4,"victus",2));
  aspect(aspects,"thread_simple",Map.of("pannus",1));aspect(aspects,"thread_gold",Map.of("pannus",1,"lucrum",1));aspect(aspects,"thread_thaumium",Map.of("pannus",1,"metallum",1,"praecantatio",1));
  aspect(aspects,"cloth_space",Map.of("pannus",3,"vacuos",3));aspect(aspects,"cloth_golden",Map.of("pannus",3,"lucrum",3));aspect(aspects,"cloth_bewitched",Map.of("pannus",3,"praecantatio",2,"vitium",1));aspect(aspects,"wolf_pelt",Map.of("pannus",2,"bestia",3));
  aspect(aspects,"photo_plate",Map.of("vitreus",5,"sensus",3,"permutatio",2,"venenum",1));aspect(aspects,"developed_photo",Map.of("aer",2,"aqua",2,"ordo",2,"sensus",2,"cognitio",2));
  aspect(aspects,"sweetwart",Map.of("ignis",1,"fames",1));aspect(aspects,"brainjerky",Map.of("cognitio",3,"fames",2,"corpus",2));
  jobs.add(d.save(c,"data/witchinggadgets/item_aspects/original_direct.json",map("format",1,"entries",aspects)));
 }
 static void aspect(List<Object> out,String item,Map<String,Integer> values){out.add(map("items",List.of("witchinggadgets:"+item),"aspects",values.entrySet().stream().map(e->map("aspect",e.getKey(),"amount",e.getValue())).toList()));}
}
