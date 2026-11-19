package thmodern.creature;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;
/** Separate named modern eggs replace the old metadata-indexed spawn list. */
public final class HorizonsSpawnEggs {
 public record Definition(String name,Supplier<? extends EntityType<? extends Mob>> type,int primary){}
 public static final List<Definition> DEFINITIONS=List.of(
  new Definition("chromatic_sheep",Horizons.CHROMATIC_SHEEP,16777215),new Definition("self_shearing_sheep",Horizons.SELF_SHEARING_SHEEP,16777215),
  new Definition("guardian_panther",Horizons.GUARDIAN_PANTHER,16761600),new Definition("gravekeeper",Horizons.GRAVEKEEPER,16776960),new Definition("familiar",Horizons.FAMILIAR,0),
  new Definition("chocolate_cow",Horizons.CHOCOLATE_COW,8667136),new Definition("scholar_chicken",Horizons.SCHOLAR_CHICKEN,0),new Definition("gold_chicken",Horizons.GOLD_CHICKEN,13873152),
  new Definition("ore_pig",Horizons.ORE_PIG,9671571),new Definition("taint_eater_pig",Horizons.TAINT_EATER_PIG,3801179),new Definition("endersteed",Horizons.ENDERSTEED,10813695),
  new Definition("seawolf",Horizons.SEAWOLF,32),new Definition("nether_hound",Horizons.NETHER_HOUND,16714752),new Definition("lunar_wolf",Horizons.LUNAR_WOLF,16777215),
  new Definition("undead_horse",Horizons.UNDEAD_HORSE,16777215),new Definition("skeletal_horse",Horizons.SKELETAL_HORSE,16777215),new Definition("nightmare",Horizons.NIGHTMARE,16777215),
  new Definition("sheeder",Horizons.SHEEDER,16777215),new Definition("soul",Horizons.SOUL,16777215));
 public static final Map<String,RegistryObject<ForgeSpawnEggItem>> EGGS=create();
 private static Map<String,RegistryObject<ForgeSpawnEggItem>> create(){var map=new LinkedHashMap<String,RegistryObject<ForgeSpawnEggItem>>();for(var definition:DEFINITIONS)map.put(definition.name(),Horizons.ITEMS.register(definition.name()+"_spawn_egg",()->new ForgeSpawnEggItem(definition.type(),definition.primary(),10813695,new Item.Properties())));return Collections.unmodifiableMap(map);}
 public static void register(){}
}
