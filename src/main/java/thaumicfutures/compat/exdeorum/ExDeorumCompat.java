package thaumicfutures.compat.exdeorum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thedarkcolour.exdeorum.block.BarrelBlock;
import thedarkcolour.exdeorum.item.HammerItem;
import thaumicfutures.FutureFeatures;

/** The Ex Astris Thaumcraft progression on Ex Deorum's native processing machinery. */
public final class ExDeorumCompat {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "thaumicfutures");
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "thaumicfutures");
    public static final RegistryObject<Block> BARREL = BLOCKS.register("thaumium_barrel", ThaumiumBarrel::new);
    public static final RegistryObject<BlockEntityType<ThaumiumBarrelEntity>> BARREL_ENTITY = TILES.register("thaumium_barrel",
            () -> BlockEntityType.Builder.of(ThaumiumBarrelEntity::new, BARREL.get()).build(null));
    public static RegistryObject<Item> DOLL, PECH_DOLL, FREEZING_DOLL, PORTAL;
    private ExDeorumCompat() {}
    public static boolean enabled() { return FutureFeatures.Group.EX_DEORUM.enabled(); }
    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        DOLL = items.register("porcelain_doll", () -> new Item(new Item.Properties()));
        PECH_DOLL = items.register("pech_doll", () -> new Item(new Item.Properties()));
        PORTAL = items.register("eldritch_portal_spawner", EldritchPortalSpawner::new);
        items.register("thaumium_barrel", () -> new BlockItem(BARREL.get(), new Item.Properties()));
        items.register("thaumium_hammer", () -> new HammerItem(Tiers.DIAMOND, new Item.Properties()) {
            @Override public boolean isValidRepairItem(ItemStack tool, ItemStack material) {
                return net.minecraft.resources.ResourceLocation.parse("thaumcraft:thaumium_ingot").equals(ForgeRegistries.ITEMS.getKey(material.getItem()));
            }
        });
        if (ModList.get().isLoaded("thermal"))
            FREEZING_DOLL = items.register("freezing_doll", () -> new Item(new Item.Properties()));
        if (ModList.get().isLoaded("forestry") && ModList.get().isLoaded("mobees")) {
            for (String name : new String[]{"bee_trap", "infused_bee_trap"}) {
                var block = BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.copy(Blocks.HAY_BLOCK).strength(.8f)));
                items.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
            }
        }
        BLOCKS.register(bus);
        TILES.register(bus);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExDeorumClient.register(bus));
    }
    private static class ThaumiumBarrel extends BarrelBlock {
        ThaumiumBarrel() { super(BlockBehaviour.Properties.of().strength(4).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ThaumiumBarrelEntity(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return type == BARREL_ENTITY.get() ? (world, pos, current, tile) -> ((ThaumiumBarrelEntity) tile).tick() : null;
        }
    }
}
