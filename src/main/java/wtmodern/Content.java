package wtmodern;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.*;

public class Content {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"warptheory");
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"warptheory");
    public static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"warptheory");
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"warptheory");
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,"warptheory");
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"warptheory");
    public static final RegistryObject<Item> CLEANSER=ITEMS.register("pure_tear",()->new WarpItem("cleanser",16));
    public static final RegistryObject<Item> SOMETHING=ITEMS.register("something",()->new WarpItem("something",64));
    public static final RegistryObject<Item> PAPER=ITEMS.register("litmus_paper",()->new WarpItem("paper",64));
    public static final RegistryObject<Item> AMULET=ITEMS.register("pure_talisman",PureTalisman::new);
    public static final RegistryObject<VanishingBlock> VANISH=BLOCKS.register("vanish",VanishingBlock::new);
    public static final RegistryObject<BlockEntityType<VanishingBlock.Snapshot>> SNAPSHOT=TILES.register("vanish",()->BlockEntityType.Builder.of(VanishingBlock.Snapshot::new,VANISH.get()).build(null));
    public static final RegistryObject<EntityType<PassiveCreeper>> FRIEND=ENTITIES.register("friend",()->EntityType.Builder.of(PassiveCreeper::new,MobCategory.CREATURE).sized(.6f,1.7f).build("warptheory:friend"));
    public static final RegistryObject<SimpleParticleType> BLOOD=PARTICLES.register("blood",()->new SimpleParticleType(false));
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.literal("Warp Theory")).icon(()->new ItemStack(CLEANSER.get())).displayItems((p,o)->ITEMS.getEntries().forEach(e->o.accept(e.get()))).build());
}
