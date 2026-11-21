package thmodern.creature;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;
public final class PlanarSlimes {
 private PlanarSlimes(){}
 public static final Map<String,RegistryObject<EntityType<PlanarSlime>>> TYPES=create();
 private static Map<String,RegistryObject<EntityType<PlanarSlime>>> create(){var types=new LinkedHashMap<String,RegistryObject<EntityType<PlanarSlime>>>();for(String kind:List.of("meat","mercurial","volt","med")){
  var type=Horizons.MOBS.register(kind+"_slime",()->EntityType.Builder.<PlanarSlime>of((t,l)->new PlanarSlime(t,l,kind),MobCategory.MONSTER).sized(.52f,.52f).clientTrackingRange(8).build(Horizons.ID+":"+kind+"_slime"));types.put(kind,type);
  Horizons.ITEMS.register(kind+"_slime_spawn_egg",()->new ForgeSpawnEggItem(type,0xffffff,0xa500ff,new Item.Properties()));
 }return Collections.unmodifiableMap(types);}
 public static void register(){}
}
