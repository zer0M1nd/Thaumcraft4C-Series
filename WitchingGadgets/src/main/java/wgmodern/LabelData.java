package wgmodern;
import java.util.*;import java.util.concurrent.CompletableFuture;import net.minecraft.data.CachedOutput;import static wgmodern.WGData.*;
public class LabelData {
 public static void add(WGData d,CachedOutput c,List<CompletableFuture<?>> jobs,List<Object> research,List<Object> pages){
  jobs.add(d.save(c,"assets/witchinggadgets/blockstates/label_library.json",map("variants",map("",map("model","witchinggadgets:block/label_library")))));
  jobs.add(d.save(c,"assets/witchinggadgets/models/block/label_library.json",map("textures",map("particle","thaumcraft:block/planks_greatwood"))));
  jobs.add(d.save(c,"assets/witchinggadgets/models/item/label_library.json",map("parent","builtin/entity","textures",map("particle","thaumcraft:block/planks_greatwood"),"display",map("gui",map("rotation",List.of(30,225,0),"scale",List.of(.625,.625,.625)),"ground",map("translation",List.of(0,3,0),"scale",List.of(.25,.25,.25)),"fixed",map("scale",List.of(.5,.5,.5)),"thirdperson_righthand",map("rotation",List.of(75,45,0),"translation",List.of(0,2.5,0),"scale",List.of(.375,.375,.375)),"firstperson_righthand",map("rotation",List.of(0,45,0),"scale",List.of(.4,.4,.4))))));
  jobs.add(d.save(c,"data/witchinggadgets/loot_tables/blocks/label_library.json",map("type","minecraft:block","pools",List.of(map("rolls",1,"entries",List.of(map("type","minecraft:item","name","witchinggadgets:label_library")))))));
  d.arcane(c,jobs,"label_library","LABELLIB","witchinggadgets:label_library",1,Map.of("ordo",30),List.of("BLW"," T "),map("B",item("minecraft:book"),"L",item("thaumcraft:jar_label"),"W",item("thaumcraft:scribing_tools"),"T",item("thaumcraft:table")));
  research.add(research("LABELLIB","label_library",4,-3,2,Map.of("sensus",4,"cognitio",4,"instrumentum",2),List.of("JARLABEL"),map("isSecondary",true),2));pages.add(map("key","witchinggadgets:LABELLIB","pages",List.of(text("LABELLIB.1"),recipe("ARCANE_CRAFTING","label_library"))));
 }
}
