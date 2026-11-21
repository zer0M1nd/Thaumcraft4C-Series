package wgmodern;
import java.util.*;import java.util.concurrent.CompletableFuture;import net.minecraft.data.CachedOutput;import static wgmodern.WGData.*;
public class PumpData {
 public static void add(WGData d,CachedOutput c,List<CompletableFuture<?>> jobs,List<Object> research,List<Object> pages){
  jobs.add(d.save(c,"assets/witchinggadgets/blockstates/mirror_pump.json",map("variants",map("",map("model","witchinggadgets:block/mirror_pump")))));
  jobs.add(d.save(c,"assets/witchinggadgets/models/block/mirror_pump.json",map("textures",map("particle","thaumcraft:block/metalbase"))));
  jobs.add(d.save(c,"assets/witchinggadgets/models/item/mirror_pump.json",map("parent","builtin/entity","textures",map("particle","thaumcraft:block/metalbase"),"display",map("gui",map("rotation",List.of(30,225,0),"scale",List.of(.625,.625,.625)),"ground",map("translation",List.of(0,3,0),"scale",List.of(.25,.25,.25)),"fixed",map("scale",List.of(.5,.5,.5)),"thirdperson_righthand",map("rotation",List.of(75,45,0),"translation",List.of(0,2.5,0),"scale",List.of(.375,.375,.375)),"firstperson_righthand",map("rotation",List.of(0,45,0),"scale",List.of(.4,.4,.4))))));
  jobs.add(d.save(c,"data/witchinggadgets/loot_tables/blocks/mirror_pump.json",map("type","minecraft:block","pools",List.of(map("rolls",1,"entries",List.of(map("type","minecraft:item","name","witchinggadgets:mirror_pump")))))));
  d.arcane(c,jobs,"mirror_pump","MIRRORPUMP","witchinggadgets:mirror_pump",1,Map.of("aer",10,"aqua",10,"ordo",10),List.of(" B ","HCT"," B "),map("B",item("thaumcraft:bellows"),"H",item("minecraft:hopper"),"C",item("thaumcraft:alchemical_construct"),"T",item("thaumcraft:essentia_tube")));
  research.add(research("MIRRORPUMP","mirror_pump",2,-8,2,Map.of("instrumentum",1,"aqua",1,"iter",1,"limus",1),List.of("MIRRORESSENTIA"),map(),2));pages.add(map("key","witchinggadgets:MIRRORPUMP","pages",List.of(text("MIRRORPUMP.1"),recipe("ARCANE_CRAFTING","mirror_pump"))));
 }
}
