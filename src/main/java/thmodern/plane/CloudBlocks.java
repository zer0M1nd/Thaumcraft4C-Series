package thmodern.plane;
import java.util.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;
public final class CloudBlocks {
 public static final Map<String,RegistryObject<Block>> BLOCKS=create();
 private static Map<String,RegistryObject<Block>> create(){var map=new LinkedHashMap<String,RegistryObject<Block>>();for(var type:CloudTypes.ALL){RegistryObject<Block> block=Horizons.BLOCKS.register(type.id(),()->new CloudBlock(type));map.put(type.id(),block);Horizons.ITEMS.register(type.id(),()->new BlockItem(block.get(),new Item.Properties()));}return Collections.unmodifiableMap(map);}
 public static final RegistryObject<BlockEntityType<CloudEntity>> ENTITY=Horizons.ENTITIES.register("cloud",()->BlockEntityType.Builder.of(CloudEntity::new,BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
 public static final RegistryObject<Item> MEAT=Horizons.ITEMS.register("meat",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationMod(.3f).build())));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<FiniteLightning>> LIGHTNING=Horizons.MOBS.register("finite_lightning",()->net.minecraft.world.entity.EntityType.Builder.<FiniteLightning>of(FiniteLightning::new,net.minecraft.world.entity.MobCategory.MISC).sized(0,0).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE).build("thaumichorizons:finite_lightning"));
 public static final RegistryObject<Item> COOKED_MEAT=Horizons.ITEMS.register("cooked_meat",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(7).saturationMod(.7f).build())));
 public static final RegistryObject<Item> MEAT_NUGGET=Horizons.ITEMS.register("meat_nugget",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationMod(.1f).build())));
 public static void register(){}
}
