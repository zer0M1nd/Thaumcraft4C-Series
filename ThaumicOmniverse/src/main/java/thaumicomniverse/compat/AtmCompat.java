package thaumicomniverse.compat;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** ATM additions only register when Allthemodium is present. */
public final class AtmCompat {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "thaumicomniverse");

    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        for (String material : new String[]{"thaumium", "void", "alchemical_brass"}) {
            // Core supplies thaumium; restoration supplies brass even without ATM.
            int first = material.equals("void") ? 0 : 1;
            int last = material.equals("void") ? 3 : 4;
            for (int tier = first; tier <= last; tier++) {
                String name = name(material, tier);
                var block = BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                        .strength(5, 10).requiresCorrectToolForDrops().sound(SoundType.METAL)));
                items.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
            }
        }
        items.register("thaumaturge_certificate", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
        BLOCKS.register(bus);
    }

    public static String name(String material, int tier) {
        return material + "_block" + (tier == 0 ? "" : "_" + tier + "x");
    }
    /** Supplies the already registered base block to material systems without creating a duplicate. */
    public static Block baseBlock(String material) {
        if(material.equals("alchemical_brass"))return java.util.Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumicfutures:alchemical_brass_block")));
        return BLOCKS.getEntries().stream().filter(entry->entry.getId().getPath().equals(name(material,0)))
                .findFirst().orElseThrow().get();
    }
}
