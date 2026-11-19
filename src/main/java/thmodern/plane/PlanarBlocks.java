package thmodern.plane;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;

/** Environment blocks retain their separate legacy drops and names. */
public final class PlanarBlocks {
 private PlanarBlocks(){}
 public static final RegistryObject<Block> LEVIATHAN=register("leviathan",LeviathanBlock::new);
 public static final RegistryObject<Block> DUST=register("dust",PlanarDustBlock::new);
 public static final RegistryObject<Block> BONE=register("bone",()->new Block(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.STONE)));
 public static final RegistryObject<Block> FLESH=register("flesh",()->soft());
 public static final RegistryObject<Block> BRAIN=register("brain",()->soft());
 public static final RegistryObject<Block> EYES=register("eyes",()->soft());
 public static final RegistryObject<Block> VOID=register("void",()->new Block(BlockBehaviour.Properties.of().strength(-1,60000).lightLevel(s->15).noLootTable().sound(new SoundType(0,1,net.minecraft.sounds.SoundEvents.WOOL_BREAK,net.minecraft.sounds.SoundEvents.WOOL_STEP,net.minecraft.sounds.SoundEvents.WOOL_PLACE,net.minecraft.sounds.SoundEvents.WOOL_HIT,net.minecraft.sounds.SoundEvents.WOOL_FALL))));
 private static Block soft(){return new Block(BlockBehaviour.Properties.of().strength(.5f).sound(SoundType.STONE));}
 private static RegistryObject<Block> register(String name,java.util.function.Supplier<Block> factory){var block=Horizons.BLOCKS.register(name,factory);Horizons.ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));return block;}
 public static void register(){} // Called after the owning deferred registers exist, before bus registration.
}
