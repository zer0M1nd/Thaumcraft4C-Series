package thmodern.plane;
import java.util.List;
/** Stable names retain the ten legacy cloud variants without registry ordinals. */
public final class CloudTypes {
 public record Type(String id,String texture,int interval,boolean glowing,int color){}
 public static final List<Type> ALL=List.of(
  new Type("cloud","cloud",-1,false,0x2040ff),new Type("fire_cloud","firecloud",-1,true,0xffffff),
  new Type("thunder_cloud","thundercloud",120,false,0x2040ff),new Type("acid_cloud","acidcloud",-1,false,0x20ff40),
  new Type("alloy_cloud","alloycloud",480,true,0x404040),new Type("flesh_cloud","fleshcloud",480,false,0xff4020),
  new Type("vis_cloud","viscloud",80,true,0xaa40c8),new Type("glyph_cloud","glyphcloud",360,true,0xffffff),
  new Type("spore_cloud","sporecloud",280,false,0xa0ffa0),new Type("animus_cloud","animuscloud",550,true,0xffe640));
 public static boolean burns(Type type){return type.id().equals("fire_cloud")||type.id().equals("alloy_cloud");}
}
