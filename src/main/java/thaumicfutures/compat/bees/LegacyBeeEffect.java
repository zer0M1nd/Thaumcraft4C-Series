package thaumicfutures.compat.bees;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IGenome;
import forestry.apiculture.genetics.effects.ThrottledBeeEffect;
import forestry.core.genetics.EffectData;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;

public final class LegacyBeeEffect implements IBeeEffect {
    private static final GameProfile OWNER = new GameProfile(UUID.fromString("2b9f5162-6099-4cee-b620-ec4278aab403"), "[MagicBees]");
    private final String kind;
    private final int delay;
    public LegacyBeeEffect(String kind) {
        this.kind = kind;
        delay = switch (kind) { case "batty" -> 300; case "ghastly" -> 2060; case "wolf" -> 650; case "transmuting" -> 200;
            case "brainy" -> 800; case "chicken" -> 20; case "cow" -> 640; case "pig" -> 350;
            default -> throw new IllegalArgumentException(kind); };
    }
    @Override public boolean isDominant() { return java.util.Set.of("transmuting", "chicken", "cow", "pig").contains(kind); }
    @Override public IEffectData validateStorage(IEffectData storage) {
        return storage instanceof EffectData data && data.getIntSize() >= 1 ? data : new EffectData(1, 0);
    }
    @Override public IEffectData doEffect(IGenome genome, IEffectData storage, IBeeHousing housing) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return storage;
        storage = validateStorage(storage);
        if (!(housing.getWorldObj() instanceof ServerLevel level)) return storage;
        int elapsed = storage.getInteger(0);
        if (elapsed < delay) { storage.setInteger(0, elapsed + 1); return storage; }
        storage.setInteger(0, 0);
        var area = ThrottledBeeEffect.getBounding(housing, genome);
        if (kind.equals("transmuting")) {
            var pos = BlockPos.containing(area.minX + level.random.nextDouble() * area.getXsize(),
                    area.minY + level.random.nextDouble() * area.getYsize(),
                    area.minZ + level.random.nextDouble() * area.getZsize());
            transmute(level, pos, housing);
        } else spawn(level, area, housing, genome);
        return storage;
    }
    private void spawn(ServerLevel level, AABB area, IBeeHousing housing, IGenome genome) {
        int roll = kind.equals("ghastly") ? 10 : kind.equals("wolf") ? 40 : 100;
        if (level.random.nextInt(100) > roll) {
            if (level.random.nextInt(100) < 35) level.playSound(null, housing.getCoordinates(),
                    kind.equals("ghastly") ? SoundEvents.GHAST_AMBIENT : SoundEvents.WOLF_PANT,
                    SoundSource.NEUTRAL, .5f, 1 + (level.random.nextFloat() - level.random.nextFloat()) * .2f);
            return;
        }
        String entity = switch (kind) {
            case "ghastly" -> "minecraft:ghast";
            case "brainy" -> "thaumcraft:brainy_zombie";
            case "chicken", "cow", "pig" -> "minecraft:" + kind;
            default -> "minecraft:wolf";
        };
        Player target = null;
        if (kind.equals("brainy")) {
            target = ThrottledBeeEffect.getEntitiesInRange(genome, housing, Player.class).stream()
                    .filter(player -> !player.isSpectator() && !player.isCreative() && (BeeManager.armorApiaristHelper == null
                            || BeeManager.armorApiaristHelper.wearsItems(player, this, true) < 4)).findFirst().orElse(null);
            if (target == null) return;
        }
        if (kind.equals("batty")) {
            boolean playerNear = ThrottledBeeEffect.getEntitiesInRange(genome, housing, Player.class).stream()
                    .anyMatch(player -> !player.isSpectator() && (BeeManager.armorApiaristHelper == null
                            || BeeManager.armorApiaristHelper.wearsItems(player, this, true) < 4));
            entity = playerNear ? "thaumcraft:firebat" : "minecraft:bat";
        }
        if ((entity.endsWith(":ghast") || entity.endsWith(":firebat") || kind.equals("brainy"))
                && level.getDifficulty() == Difficulty.PEACEFUL) return;
        double x = area.minX + level.random.nextDouble() * area.getXsize();
        double y = housing.getCoordinates().getY() + level.random.nextInt(3) - 1;
        double z = area.minZ + level.random.nextDouble() * area.getZsize();
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(pos)) return;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entity));
        if (type == null || !(type.create(level) instanceof Mob mob)) return;
        int cap = switch (kind) {
            case "ghastly" -> 1; case "wolf", "brainy" -> 2; case "chicken" -> 20;
            case "cow" -> 3; case "pig" -> 4; default -> 6;
        };
        if (level.getEntitiesOfClass(mob.getClass(), new AABB(pos).inflate(8, 4, 8)).size() >= cap) return;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360, 0);
        if (!mob.checkSpawnRules(level, MobSpawnType.MOB_SUMMONED) || !mob.checkSpawnObstruction(level)) return;
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        if (!mob.isSpawnCancelled() && level.addFreshEntity(mob) && target != null) mob.setTarget(target);
    }
    private static void transmute(ServerLevel level, BlockPos pos, IBeeHousing housing) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return;
        var state = level.getBlockState(pos);
        var biome = level.getBiome(pos);
        var candidates = new ArrayList<Block>();
        if (state.is(Blocks.SAND) && (biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:is_sandy")))
                || biome.is(net.minecraft.world.level.biome.Biomes.DESERT))) candidates.add(Blocks.SANDSTONE);
        if (state.is(BlockTags.LOGS)) add(candidates, "botania:livingwood_log");
        if (state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse("forge:stone")))) add(candidates, "botania:livingrock");
        if (state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse("botania:livingwood_logs")))
                || ForgeRegistries.BLOCKS.getKey(state.getBlock()).equals(ResourceLocation.parse("botania:livingwood_log")))
            add(candidates, "botania:dreamwood_log");
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)) {
            if (biome.is(BiomeTags.IS_FOREST) && !biome.value().coldEnoughToSnow(pos)) add(candidates, "railcraft:quarried_stone");
            if (biome.is(BiomeTags.IS_OCEAN)) add(candidates, "railcraft:abyssal_stone");
        }
        candidates.removeIf(candidate -> candidate == state.getBlock());
        if (candidates.isEmpty()) return;
        var player = FakePlayerFactory.get(level, housing.getOwner() == null ? OWNER : housing.getOwner());
        if (!level.mayInteract(player, pos)) return;
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        var replacement = candidates.get(level.random.nextInt(candidates.size())).defaultBlockState();
        if (!level.setBlock(pos, replacement, 3)) return;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) snapshot.restore(true, false);
    }
    private static Block block(String id) { return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id)); }
    private static void add(ArrayList<Block> values, String id) {
        Block block = block(id);
        if (block != null && block != Blocks.AIR) values.add(block);
    }
}
