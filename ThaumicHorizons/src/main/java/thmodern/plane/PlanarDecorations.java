package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;
public final class PlanarDecorations {
 public static final Map<String,RegistryObject<Block>> CRYSTALS=crystals();
 public static final Map<String,RegistryObject<Block>> SPIKES=spikes();
 private static Map<String,RegistryObject<Block>> crystals(){var map=new LinkedHashMap<String,RegistryObject<Block>>();for(var color:DyeColor.values()){
  RegistryObject<Block> block=Horizons.BLOCKS.register("crystal_"+color.getName(),()->new GlassBlock(Block.Properties.of().mapColor(color).strength(1.75f,.5f).sound(SoundType.GLASS).noOcclusion()){@Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 1;}});
  map.put(color.getName(),block);Horizons.ITEMS.register("crystal_"+color.getName(),()->new BlockItem(block.get(),new Item.Properties()));
 }return Collections.unmodifiableMap(map);}
 public static final RegistryObject<Block> DEEP_CRYSTAL=Horizons.BLOCKS.register("deep_crystal",()->new GlassBlock(Block.Properties.of().strength(2).sound(SoundType.GLASS).noOcclusion().lightLevel(s->15)));
 static{Horizons.ITEMS.register("deep_crystal",()->new BlockItem(DEEP_CRYSTAL.get(),new Item.Properties()));}
 private static Map<String,RegistryObject<Block>> spikes(){var map=new LinkedHashMap<String,RegistryObject<Block>>();for(String kind:List.of("metal","wooden","tooth")){
  RegistryObject<Block> block=Horizons.BLOCKS.register(kind+"_spike",()->new SpikeBlock(kind));map.put(kind,block);Horizons.ITEMS.register(kind+"_spike",()->new SpikeBlockItem(block.get()));
 }return Collections.unmodifiableMap(map);}
 public static final RegistryObject<BlockEntityType<SpikeEntity>> SPIKE_ENTITY=Horizons.ENTITIES.register("spike",()->BlockEntityType.Builder.of(SpikeEntity::new,SPIKES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
 public static final class SpikeEntity extends BlockEntity{public SpikeEntity(BlockPos pos,BlockState state){super(SPIKE_ENTITY.get(),pos,state);}}
 public static void register(){}
}
