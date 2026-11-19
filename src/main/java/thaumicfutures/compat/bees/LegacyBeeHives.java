package thaumicfutures.compat.bees;

import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.hives.IHiveDefinition;
import forestry.api.apiculture.hives.IHiveGen;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.alleles.BeeChromosomes;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.plugin.IApicultureRegistration;
import forestry.apiculture.blocks.BlockBeeHive;
import forestry.apiculture.tiles.TileHive;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Native Forestry hives with a separate valid-block set for their tile type. */
public final class LegacyBeeHives {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "thaumicfutures");
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "thaumicfutures");
    private static final RegistryObject<Block> DEEP = BLOCKS.register("bee_hive_deep", () -> new Hive("attuned", 4));
    private static final RegistryObject<Block> INFERNAL = BLOCKS.register("bee_hive_infernal", () -> new Hive("infernal", 15));
    private static final RegistryObject<Block> CURIOUS = BLOCKS.register("bee_hive_curious", () -> new Hive("mystical", 12));
    private static final RegistryObject<Block> UNUSUAL = BLOCKS.register("bee_hive_unusual", () -> new Hive("unusual", 12));
    private static final RegistryObject<Block> RESONANT = BLOCKS.register("bee_hive_resonant", () -> new Hive("sorcerous", 12));
    private static final RegistryObject<Block> OBLIVION = BLOCKS.register("bee_hive_oblivion", () -> new Hive("oblivion", 7));
    private static final RegistryObject<BlockEntityType<TileHive>> TILE = TILES.register("bee_hive",
            () -> BlockEntityType.Builder.of(LegacyBeeHives::tile, DEEP.get(), INFERNAL.get(), CURIOUS.get(), UNUSUAL.get(), RESONANT.get(), OBLIVION.get()).build(null));

    private LegacyBeeHives() {}
    private static TileHive tile(BlockPos pos, BlockState state) { return new TileHive(TILE.get(), pos, state); }
    private static ResourceLocation species(String name) { return ThaumicBeeMutations.parent(name); }
    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        items.register("bee_hive_deep", () -> new BlockItem(DEEP.get(), new Item.Properties()));
        items.register("bee_hive_infernal", () -> new BlockItem(INFERNAL.get(), new Item.Properties()));
        items.register("bee_hive_curious", () -> new BlockItem(CURIOUS.get(), new Item.Properties()));
        items.register("bee_hive_unusual", () -> new BlockItem(UNUSUAL.get(), new Item.Properties()));
        items.register("bee_hive_resonant", () -> new BlockItem(RESONANT.get(), new Item.Properties()));
        items.register("bee_hive_oblivion", () -> new BlockItem(OBLIVION.get(), new Item.Properties()));
        BLOCKS.register(bus);
        TILES.register(bus);
    }

    public static void apiculture(IApicultureRegistration registration) {
        for (var entry : Map.of("mystical", CURIOUS, "unusual", UNUSUAL, "sorcerous", RESONANT).entrySet()) {
            var hive = registration.registerHive(species(entry.getKey()), new CraftedDefinition(entry.getValue().get()));
            hive.addDrop(.8, species(entry.getKey()), () -> List.of(MagicBeeProducts.stack("bee_comb_legacy_mundane")), .7f);
            hive.addDrop(entry.getKey().equals("sorcerous") ? .2 : .15, species(entry.getKey()),
                    () -> List.of(MagicBeeProducts.stack("bee_comb_legacy_mundane")), 0,
                    Map.of(BeeChromosomes.TOLERATES_RAIN, ForestryAlleles.TRUE));
            hive.addDrop(.05, ForestryBeeSpecies.VALIANT, () -> List.of(MagicBeeProducts.stack("bee_comb_legacy_mundane")), 0,
                    Map.of(BeeChromosomes.TOLERATES_RAIN, ForestryAlleles.TRUE));
            hive.setGenerationChance(0);
        }
        var oblivion = registration.registerHive(species("oblivion"), new CraftedDefinition(OBLIVION.get()));
        oblivion.addDrop(.8, species("oblivion"), LegacyBeeHives::oblivionExtras, 0);
        oblivion.addDrop(.09, ForestryBeeSpecies.STEADFAST, LegacyBeeHives::oblivionExtras);
        oblivion.setGenerationChance(0);
        var deep = registration.registerHive(species("attuned"), new Definition(Kind.DEEP));
        deep.addDrop(.8, species("attuned"), () -> List.of(MagicBeeProducts.stack("bee_comb_legacy_mundane")), .65f);
        deep.addDrop(.2, species("attuned"), () -> List.of(MagicBeeProducts.stack("bee_comb_legacy_mundane")), 0,
                Map.of(BeeChromosomes.TOLERATES_RAIN, ForestryAlleles.TRUE));
        deep.addDrop(.05, ForestryBeeSpecies.VALIANT, () -> List.of(MagicBeeProducts.stack("bee_comb_legacy_mundane")), 0,
                Map.of(BeeChromosomes.TOLERATES_RAIN, ForestryAlleles.TRUE));
        deep.setGenerationChance(5);
        for (Kind kind : new Kind[]{Kind.NETHER, Kind.HOT_OVERWORLD}) {
            // Definitions need unique keys, while the block always uses the infernal drop table.
            var hive = registration.registerHive(kind == Kind.NETHER ? species("infernal")
                    : MagicBeesPlugin.key("hive_infernal_overworld"), new Definition(kind));
            hive.addDrop(.8, species("infernal"), LegacyBeeHives::infernalExtras, .5f);
            hive.addDrop(.03, ForestryBeeSpecies.STEADFAST, LegacyBeeHives::infernalExtras);
            hive.setGenerationChance(kind == Kind.NETHER ? 50 : .95f);
        }
    }
    private static List<ItemStack> infernalExtras() {
        return List.of(MagicBeeProducts.stack("bee_comb_legacy_molten"), new ItemStack(Items.GLOWSTONE_DUST, 6));
    }
    private static List<ItemStack> oblivionExtras() {
        return List.of(MagicBeeProducts.stack("bee_comb_legacy_forgotten"), new ItemStack(Items.ENDER_PEARL));
    }
    /** These four hives enter the world through Ex Astris recipes; existing world generation is unchanged. */
    private record CraftedDefinition(Block block) implements IHiveDefinition, IHiveGen {
        public IHiveGen getHiveGen() { return this; }
        public BlockState getBlockState() { return block.defaultBlockState(); }
        public boolean isGoodBiome(Holder<Biome> biome) { return false; }
        public boolean isGoodHumidity(HumidityType humidity) { return false; }
        public boolean isGoodTemperature(TemperatureType temperature) { return false; }
        public float getGenChance() { return 0; }
        public BlockPos getPosForHive(WorldGenLevel level, int x, int z) { return null; }
        public boolean canReplace(BlockState state, WorldGenLevel level, BlockPos pos) { return false; }
        public boolean isValidLocation(WorldGenLevel level, BlockPos pos) { return false; }
        public void postGen(WorldGenLevel level, RandomSource random, BlockPos pos) {}
    }

    private static final class Hive extends BlockBeeHive {
        private final int light;
        Hive(String species, int light) { super(LegacyBeeHives.species(species)); this.light = light; }
        @Override public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) { return light; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return tile(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return type == TILE.get() ? (world, pos, current, entity) -> ((TileHive) entity).tick(world) : null;
        }
    }
    private enum Kind { DEEP, NETHER, HOT_OVERWORLD }
    private static boolean biomeTag(Holder<Biome> biome, String id) {
        return biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(id)));
    }
    private static final class Definition implements IHiveDefinition, IHiveGen {
        private final Kind kind;
        private final int min, range, neighbors;
        Definition(Kind kind) {
            this.kind = kind;
            min = kind == Kind.DEEP ? 10 : kind == Kind.NETHER ? 0 : 5;
            range = kind == Kind.DEEP ? 15 : kind == Kind.NETHER ? 175 : 13;
            neighbors = kind == Kind.DEEP ? 5 : 6;
        }
        @Override public IHiveGen getHiveGen() { return this; }
        @Override public BlockState getBlockState() { return (kind == Kind.DEEP ? DEEP : INFERNAL).get().defaultBlockState(); }
        @Override public boolean isGoodBiome(Holder<Biome> biome) {
            if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return false;
            if (kind == Kind.NETHER) return biome.is(BiomeTags.IS_NETHER);
            if (biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_END)) return false;
            boolean magical = biomeTag(biome, "thaumicomniverse:magical");
            return magical || (kind == Kind.DEEP
                    ? biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)
                    : biomeTag(biome, "forge:is_hot"));
        }
        @Override public boolean isGoodHumidity(HumidityType humidity) { return true; }
        @Override public boolean isGoodTemperature(TemperatureType temperature) { return true; }
        @Override public float getGenChance() { if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return 0; return kind == Kind.DEEP ? 5 : kind == Kind.NETHER ? 50 : .95f; }
        @Override public BlockPos getPosForHive(WorldGenLevel level, int x, int z) { return getPosForHive(level, level.getRandom(), x, z); }
        @Override public BlockPos getPosForHive(WorldGenLevel level, RandomSource random, int x, int z) {
            int y = min + random.nextInt(range);
            BlockPos pos = new BlockPos(x, y, z);
            if (kind == Kind.NETHER && !isValidLocation(level, pos)) {
                int step = random.nextBoolean() ? 4 : -4;
                while (!substrate(level.getBlockState(pos))) {
                    y += step;
                    if (y < min || y > min + range || level.isOutsideBuildHeight(y)) return null;
                    pos = new BlockPos(x, y, z);
                }
            }
            return level.isOutsideBuildHeight(pos) ? null : pos;
        }
        private boolean substrate(BlockState state) { return state.is(kind == Kind.NETHER ? Blocks.NETHERRACK : Blocks.STONE); }
        @Override public boolean canReplace(BlockState state, WorldGenLevel level, BlockPos pos) {
            return substrate(state) && level.getBlockEntity(pos) == null;
        }
        @Override public boolean isValidLocation(WorldGenLevel level, BlockPos pos) {
            if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return false;
            int count = 0;
            for (Direction direction : Direction.values()) if (substrate(level.getBlockState(pos.relative(direction)))) count++;
            return count >= neighbors;
        }
        @Override public void postGen(WorldGenLevel level, RandomSource random, BlockPos pos) {
            // Native modern ore features replace the old vein algorithm while retaining its six origins and sizes.
            for (Direction direction : Direction.values()) {
                BlockPos origin = pos.relative(direction);
                if (kind == Kind.HOT_OVERWORLD && direction.getAxis() == Direction.Axis.Y) {
                    if (canReplace(level.getBlockState(origin), level, origin)) level.setBlock(origin, Blocks.GLOWSTONE.defaultBlockState(), 2);
                    continue;
                }
                Block ore = kind == Kind.DEEP ? Blocks.REDSTONE_ORE : kind == Kind.NETHER ? Blocks.NETHER_QUARTZ_ORE : Blocks.GLOWSTONE;
                int size = kind == Kind.DEEP ? 5 : kind == Kind.NETHER ? 4 : 1 + random.nextInt(4);
                var target = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(kind == Kind.NETHER ? Blocks.NETHERRACK : Blocks.STONE);
                var configuration = new net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration(target, ore.defaultBlockState(), size);
                net.minecraft.world.level.levelgen.feature.Feature.ORE.place(configuration, level, level.getLevel().getChunkSource().getGenerator(), random, origin);
            }
        }
    }
}
