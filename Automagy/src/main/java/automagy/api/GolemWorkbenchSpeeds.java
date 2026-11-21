package automagy.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Common-setup extension point for the workbench's speed-upgrade effectiveness. */
public class GolemWorkbenchSpeeds {
 private static final Map<String,Integer> LEGACY=Map.of("STRAW",5,"WOOD",4,"TALLOW",3,"CLAY",3,"FLESH",4,"STONE",2,"IRON",1,"THAUMIUM",2);
 private static final Map<String,Integer> CUSTOM=new ConcurrentHashMap<>();
 public static void register(String material,int speed){
  if(!material.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")||speed<0||speed>64)throw new IllegalArgumentException("Invalid workbench material/speed");
  if(CUSTOM.putIfAbsent(material,speed)!=null)throw new IllegalArgumentException("Duplicate workbench material "+material);
 }
 public static int base(String customMaterial,String legacyMaterial){
  return customMaterial==null?LEGACY.getOrDefault(legacyMaterial,2):CUSTOM.getOrDefault(customMaterial,2);
 }
}
