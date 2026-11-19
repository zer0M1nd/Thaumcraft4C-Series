package wgmodern;
import java.util.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.research.KnowledgeSavedData;
public class ResearchAliases {
 public record Alias(String original,int x,int y,String item,String texture,boolean special){}
 public static final List<Alias> ALL=List.of(new Alias("ENCHFABRIC",10,1,"thaumcraft:enchanted_fabric",null,false),new Alias("ARCANESTONE",-3,-5,"thaumcraft:arcane_stone",null,false),new Alias("BATHSALTS",-1,-3,"thaumcraft:bath_salts",null,false),new Alias("JARLABEL",4,-1,"thaumcraft:essentia_jar",null,false),new Alias("MIRRORESSENTIA",1,-7,"thaumcraft:essentia_mirror",null,false),new Alias("ALCHEMICALMANUFACTURE",-4,-4,null,"thaumcraft:textures/misc/r_alchman.png",false),new Alias("PUREIRON",-5,-2,"thaumcraft:iron_cluster",null,false),new Alias("TRANSIRON",-4,-2,"minecraft:iron_nugget",null,false),new Alias("INFERNALFURNACE",3,-5,null,"thaumcraft:textures/misc/r_infernalfurnace.png",false),new Alias("CENTRIFUGE",5,-5,"thaumcraft:alchemy_centrifuge",null,false),new Alias("INFUSIONENCHANTMENT",-8,1,null,"thaumcraft:textures/misc/r_enchant.png",false),new Alias("ELDRITCHMINOR",1,3,null,"thaumcraft:textures/misc/r_eldritchminor.png",true),new Alias("PRIMPEARL",0,1,"thaumcraft:primordial_pearl",null,true));
 public static String key(String original){return "witchinggadgets:WGFAKE"+original;}
 public static String translation(String key){for(String prefix:List.of("tc.research_name.","tc.research_text.")){String marker=prefix+"witchinggadgets:WGFAKE";if(key.startsWith(marker))return prefix+key.substring(marker.length());}return key;}
}
