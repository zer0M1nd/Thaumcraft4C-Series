package ttmodern.compat;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import ttmodern.EnchanterCatalog;
/** Optional adapter; loaded only while Thaumic Bases is present. */
public class BasesEnchantments {
 public static Map<ResourceLocation,EnchanterCatalog.Entry> entries(){
  var result=new LinkedHashMap<ResourceLocation,EnchanterCatalog.Entry>();
  for(var value:tbmodern.api.EnchantingCompatibility.entries()){
   var id=ResourceLocation.parse(value.enchantment());
   result.put(id,new EnchanterCatalog.Entry(id,"",ResourceLocation.parse(value.texture()),value.baseVis()));
  }return result;
 }
}
