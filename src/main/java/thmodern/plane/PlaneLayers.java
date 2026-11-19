package thmodern.plane;

import java.util.*;

/** Registry-independent, ordered strata. Later layers overwrite lower parts of earlier ones. */
public final class PlaneLayers {
 public record Layer(String block,int height,int noise,int life,boolean environment){}
 private PlaneLayers(){}
 public static List<Layer> of(PlaneParameters p){
  var result=new ArrayList<Layer>();int height=128,noise=(int)p.noise(),life=(int)p.life(),radius=p.radius();
  for(String aspect:List.of("gelum","aqua","vitium","pannus","corpus","terra","ignis","tutamen")){
   if(p.amount(aspect)==0)continue;
   String block=switch(aspect){case "gelum"->"minecraft:packed_ice";case "aqua"->"minecraft:water";case "vitium"->"thaumcraft:taint_soil";case "pannus"->"minecraft:white_wool";case "corpus"->"thaumcraft:flesh_block";case "terra"->p.biome().equals("minecraft:desert")?"minecraft:sand":"minecraft:dirt";case "ignis"->"minecraft:netherrack";default->"minecraft:obsidian";};
   boolean environment=Set.of("aqua","vitium","terra").contains(aspect);
   result.add(new Layer(block,height,Set.of("gelum","aqua").contains(aspect)?0:noise,environment?life:0,environment));
   if(aspect.equals("terra"))result.add(new Layer(p.amount("ordo")>=p.amount("perditio")?"minecraft:stone":"minecraft:cobblestone",height-5,noise,0,false));
   // Legacy corpus accidentally used pannus here; give flesh its own reserved thickness.
   height-=(int)Math.floor(p.fraction(aspect)*radius/(aspect.equals("aqua")?1.5:1));
  }
  if(result.isEmpty())result.add(new Layer("thaumichorizons:dust",128,noise,life,true));
  return List.copyOf(result);
 }
 /** Clip every column to the sphere, excluding y=0 as in the original generator. */
 public static int bottom(int radius,int x,int z){int squared=radius*radius-x*x-z*z;return squared<0?256:Math.max(1,128-(int)Math.sqrt(squared));}
 public static int top(int radius,int x,int z,int requested){int squared=radius*radius-x*x-z*z;return squared<0?-1:Math.min(128+(int)Math.sqrt(squared),requested);}
 public static double solidity(PlaneParameters p){return Math.max(0,Math.min(1,.5-p.fraction("aer")*.5+p.fraction("ordo")*.25-p.fraction("vacuos")-p.fraction("volatus")*.5+p.fraction("fabrico")*.5-p.fraction("vinculum")*.5));}
 public static int caveCount(PlaneParameters p){int r=p.radius();return (int)((r*r*r/10000)*(solidity(p)+.1));}
}
