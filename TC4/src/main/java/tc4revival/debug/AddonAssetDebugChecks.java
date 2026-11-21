package tc4revival.debug;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-side registry/resource contracts. This deliberately makes no rendering claim. */
public class AddonAssetDebugChecks {
 public static int verify(CommandSourceStack source){
  var namespaces=Set.of("thaumic_tinkerer","forbidden_magic","thaumicenergistics","thaumichorizons","thaumicbases","automagy");
  var failures=new ArrayList<String>();int items=0,blocks=0,states=0;
  for(var id:ForgeRegistries.ITEMS.getKeys())if(namespaces.contains(id.getNamespace())){
   items++;if(!exists(id.getNamespace(),"models","item/"+id.getPath()+".json"))failures.add("Missing item model "+id);
  }
  for(var block:ForgeRegistries.BLOCKS){
   var id=ForgeRegistries.BLOCKS.getKey(block);if(!namespaces.contains(id.getNamespace()))continue;blocks++;
   var path=find(id.getNamespace(),"blockstates",id.getPath()+".json");
   if(path==null){failures.add("Missing blockstates "+id);continue;}
   try{
    var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    if(json.has("variants")){
     var variants=json.getAsJsonObject("variants");
     for(String key:variants.keySet())if(!key.isEmpty())for(String term:key.split(",")){
      var pair=term.split("=",2);var property=block.getStateDefinition().getProperty(pair[0]);
      if(pair.length!=2||property==null||property.getValue(pair[1]).isEmpty())failures.add("Invalid variant "+id+"#"+key);
     }
     for(var state:block.getStateDefinition().getPossibleStates()){
      states++;boolean covered=false;
      for(String key:variants.keySet()){
       boolean matches=true;
       if(!key.isEmpty())for(String term:key.split(",")){
        var pair=term.split("=",2);var property=block.getStateDefinition().getProperty(pair[0]);
        if(pair.length!=2||property==null||!String.valueOf(state.getValue(property)).equalsIgnoreCase(pair[1])){matches=false;break;}
       }
       if(matches){covered=true;break;}
      }
      if(!covered){failures.add("Uncovered state "+state);break;}
     }
    }
   }catch(Exception ex){failures.add(id+": "+ex.getMessage());}
  }
  if(!failures.isEmpty()){for(var failure:failures)source.sendFailure(Component.literal(failure));return 0;}
  String result="Addon asset contracts passed: "+items+" item models; "+blocks+" blockstate files; "+states+" variant states. Client rendering not tested.";
  source.sendSuccess(()->Component.literal(result),false);return 1;
 }
 private static boolean exists(String namespace,String directory,String name){return find(namespace,directory,name)!=null;}
 private static Path find(String namespace,String directory,String name){
  for(var mod:ModList.get().getModFiles()){
   var p=mod.getFile().findResource("assets",namespace,directory,name);if(Files.isRegularFile(p))return p;
  }return null;
 }
}
