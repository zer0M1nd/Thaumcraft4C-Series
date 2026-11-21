package tbmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Four legacy trees, implemented using the modern sapling and leaf lifecycle. */
public final class Trees {
    public static final List<String> KINDS = List.of("golden_oak", "peaceful", "nether", "ender");
    static boolean nether(BlockState s) { return s.is(Blocks.NETHERRACK) || s.is(Blocks.SOUL_SAND) || s.is(Blocks.NETHER_QUARTZ_ORE); }
    static boolean end(BlockState s) { return s.is(Blocks.END_STONE) || s.is(Blocks.OBSIDIAN); }
    static boolean overworld(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(BlockTags.BASE_STONE_OVERWORLD)
            || s.getTags().anyMatch(t -> t.location().getNamespace().equals("forge") &&
                Set.of("dirt", "grass", "sand", "gravel", "stone").contains(t.location().getPath()));
    }
    public static final class Sapling extends SaplingBlock {
        private final String kind;
        public Sapling(String kind) {
            super(new AbstractTreeGrower() {
                @Override protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) {
                    return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(ThaumicBases.ID, kind + "_tree"));
                }
            }, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING));
            this.kind = kind;
        }
        @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || kind.equals("nether") && nether(state) || kind.equals("ender") && end(state);
        }
    }
    public static final class Log extends RotatedPillarBlock {
        private final String kind;
        public Log(String kind) { super(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)); this.kind = kind; }
        @Override public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
            return !(kind.equals("ender") && entity instanceof EnderDragon) && super.canEntityDestroy(state, level, pos, entity);
        }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return kind.equals("nether") ? 0 : 5; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return kind.equals("nether") ? 0 : 5; }
        @Override public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction side) { return kind.equals("nether"); }
    }
    public static final class Leaves extends LeavesBlock {
        private final String kind;
        public Leaves(String kind) { super(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).lightLevel(s -> kind.equals("ender") ? 11 : 0)); this.kind = kind; }
        @Override public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
            return !(kind.equals("ender") && entity instanceof EnderDragon) && super.canEntityDestroy(state, level, pos, entity);
        }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return kind.equals("nether") ? 0 : 60; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return kind.equals("nether") ? 0 : 60; }
        @Override public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction side) { return kind.equals("nether"); }
        @Override public boolean isRandomlyTicking(BlockState state) { return true; }
        @Override public List<ItemStack> onSheared(net.minecraft.world.entity.player.Player player, ItemStack tool, Level level, BlockPos pos, int fortune) {
            return kind.equals("golden_oak") ? List.of() : List.of(new ItemStack(this));
        }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
            ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
            if (tool == null) tool = ItemStack.EMPTY;
            if (tool.is(Items.SHEARS)) return kind.equals("golden_oak") ? List.of() : List.of(new ItemStack(this));
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) return List.of(new ItemStack(this));
            int fortune = Math.min(10, Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool)));
            RandomSource random = context.getLevel().random;
            List<ItemStack> result = new ArrayList<>();
            int rate = kind.equals("golden_oak") ? 50 : 30;
            if (fortune > 0) rate = Math.max(10, rate - (2 << fortune));
            if (random.nextInt(rate) == 0) result.add(new ItemStack(Content.item(kind + "_sapling")));
            int appleRate = fortune == 0 ? 200 : Math.max(40, 200 - (10 << fortune));
            if (kind.equals("golden_oak") && random.nextInt(appleRate) == 0) result.add(new ItemStack(Items.GOLDEN_APPLE));
            return result;
        }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            if (!level.getBlockState(pos).is(this) || kind.equals("golden_oak")) return;
            if (kind.equals("peaceful") && random.nextDouble() > .03) return;
            int depth = kind.equals("peaceful") ? 6 : kind.equals("nether") ? 8 : 11;
            for (int d = 1; d <= depth; d++) {
                BlockPos ground = pos.below(d);
                if (level.isOutsideBuildHeight(ground)) break;
                BlockState below = level.getBlockState(ground);
                if (below.isAir()) continue;
                if (kind.equals("peaceful")) { spawn(level, ground.above(), level.getBiome(ground).value(), MobCategory.CREATURE, random); break; }
                boolean special = kind.equals("nether") ? nether(below) : end(below);
                if (special && random.nextDouble() <= (kind.equals("nether") ? .05 : .02)) {
                    var biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(kind.equals("nether") ? Biomes.NETHER_WASTES : Biomes.THE_END);
                    spawn(level, ground.above(), biome, random.nextBoolean() ? MobCategory.CREATURE : MobCategory.MONSTER, random);
                    break;
                } else if (overworld(below)) {
                    float roll = random.nextFloat();
                    Block replacement = kind.equals("nether") ? (roll <= .6 ? Blocks.NETHERRACK : roll <= .9 ? Blocks.SOUL_SAND : Blocks.NETHER_QUARTZ_ORE)
                        : (roll <= .9 ? Blocks.END_STONE : Blocks.OBSIDIAN);
                    level.setBlockAndUpdate(ground, replacement.defaultBlockState()); break;
                }
            }
        }
        private static void spawn(ServerLevel level, BlockPos pos, Biome biome, MobCategory category, RandomSource random) {
            if (biome == null || category == MobCategory.MONSTER && level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
            var entries = biome.getMobSettings().getMobs(category).unwrap();
            if (entries.isEmpty()) return;
            Entity entity = entries.get(random.nextInt(entries.size())).type.create(level);
            if (!(entity instanceof Mob mob)) return;
            mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            var check = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(mob, level, MobSpawnType.MOB_SUMMONED, null);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(check);
            if (check.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
            // The original intentionally ignores light restrictions; DEFAULT still checks collisions and liquids.
            if (check.getResult() != net.minecraftforge.eventbus.api.Event.Result.ALLOW
                && (!level.noCollision(mob) || !level.isUnobstructed(mob) || level.containsAnyLiquid(mob.getBoundingBox()))) return;
            net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
            level.addFreshEntity(mob);
        }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            super.animateTick(state, level, pos, random);
            ParticleOptions particle;
            switch (kind) {
                case "golden_oak" -> particle = new DustParticleOptions(new org.joml.Vector3f(1, 1, 0), 1);
                case "peaceful" -> particle = ParticleTypes.HEART;
                case "nether" -> { if (random.nextFloat() > .01F || !level.isEmptyBlock(pos.below())) return; particle = ParticleTypes.DRIPPING_LAVA; }
                default -> particle = ParticleTypes.PORTAL;
            }
            level.addParticle(particle, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
    private Trees() {}
}
