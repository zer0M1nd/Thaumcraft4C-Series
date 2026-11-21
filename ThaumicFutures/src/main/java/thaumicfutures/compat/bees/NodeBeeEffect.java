package thaumicfutures.compat.bees;

import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IGenome;
import forestry.apiculture.genetics.Bee;
import forestry.core.genetics.EffectData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.content.AuraNodeBlockEntity;
import tc4revival.content.AuraNodeModifier;
import tc4revival.content.AuraNodeType;

/** Permanent node changes through the core API, without promoting temporary node leases. */
public final class NodeBeeEffect implements IBeeEffect {
    public enum Action {
        RECHARGE(20), EMPOWER(200), REPAIR(2), TAINT(250), PURE(250), HUNGRY(2);
        final int delay;
        Action(int delay) { this.delay = delay; }
    }
    private final Action action;
    public NodeBeeEffect(Action action) { this.action = action; }
    @Override public boolean isDominant() { return false; }
    @Override public boolean isCombinable() { return action == Action.RECHARGE || action == Action.EMPOWER; }
    @Override public IEffectData validateStorage(IEffectData storage) {
        return storage instanceof EffectData data && data.getIntSize() >= 1 ? data : new EffectData(1, 0);
    }
    @Override public IEffectData doEffect(IGenome genome, IEffectData storage, IBeeHousing housing) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return storage;
        storage = validateStorage(storage);
        if (!(housing.getWorldObj() instanceof ServerLevel level)) return storage;
        int counter = storage.getInteger(0);
        if (counter < action.delay) { storage.setInteger(0, counter + 1); return storage; }
        int radius = Math.max(1, Bee.getAdjustedTerritory(genome,
                IForestryApi.INSTANCE.getHiveManager().createBeeHousingModifier(housing)).getX());
        var groups = nodes(level, housing.getCoordinates(), radius);
        shuffle(groups, level.random);
        if (apply(groups, level.random)) storage.setInteger(0, 0);
        return storage;
    }
    private boolean apply(List<List<AuraNodeBlockEntity>> chunks, RandomSource random) {
        int attempts = 0;
        for (var group : chunks) {
            if (action == Action.RECHARGE || action == Action.REPAIR) shuffle(group, random);
            for (var node : group) {
                ++attempts;
                if (action == Action.RECHARGE) { recharge(node, random); return true; }
                if (action == Action.REPAIR) {
                    if (repair(node, random)) return true;
                    break; // Original repair attempts one random node per chunk.
                }
                if (action == Action.EMPOWER) {
                    var capacity = node.permanentBase();
                    long total = Math.max(1, capacity.total());
                    if (random.nextDouble() * total >= 120) continue;
                    Aspect aspect = null;
                    int rolls = 0;
                    do {
                        aspect = randomAspect(random);
                        if (capacity.amount(aspect) <= 255) break;
                    } while (++rolls < 20);
                    if (rolls >= 20) return false;
                    int growth = 1 + random.nextInt(2);
                    capacity.add(aspect, growth);
                    node.editPermanent(capacity, node.permanentType(), node.permanentModifier());
                    int room = Math.max(0, node.base().amount(aspect) - node.aspects().amount(aspect));
                    node.addCharge(aspect, Math.min(room, growth));
                    return true;
                }
                AuraNodeType desired = switch (action) {
                    case TAINT -> AuraNodeType.TAINTED;
                    case PURE -> AuraNodeType.PURE;
                    case HUNGRY -> AuraNodeType.HUNGRY;
                    default -> throw new IllegalStateException();
                };
                AuraNodeType current = node.permanentType();
                if (current == desired) continue;
                AuraNodeType next = current == AuraNodeType.NORMAL ? desired : AuraNodeType.NORMAL;
                float chance = switch (next) {
                    case NORMAL -> .075f;
                    case TAINTED -> .057f;
                    case PURE -> .035f;
                    case HUNGRY -> .0094f;
                    default -> 0;
                };
                if (random.nextFloat() < chance) {
                    node.editPermanent(node.permanentBase(), next, node.permanentModifier());
                    // The old helper forgot to set its changed flag; persist and synchronize successful conversions.
                    return true;
                }
            }
        }
        return action == Action.REPAIR || action != Action.RECHARGE && attempts < 10;
    }
    private static void recharge(AuraNodeBlockEntity node, RandomSource random) {
        var capacity = node.permanentBase();
        var charge = node.aspects();
        for (var aspect : charge.aspects()) {
            int missing = Math.min(capacity.amount(aspect), node.base().amount(aspect)) - charge.amount(aspect);
            if (missing > 0) node.addCharge(aspect, missing == 1 ? 1 : 1 + random.nextInt(missing - 1));
        }
    }
    private static boolean repair(AuraNodeBlockEntity node, RandomSource random) {
        var kind = node.permanentType();
        var quality = node.permanentModifier();
        if (kind == AuraNodeType.UNSTABLE) kind = AuraNodeType.NORMAL;
        else if (quality == AuraNodeModifier.FADING) quality = AuraNodeModifier.PALE;
        else if (quality == AuraNodeModifier.PALE && random.nextFloat() < .65f) quality = null;
        else if (quality == null && random.nextFloat() < .15f) quality = AuraNodeModifier.BRIGHT;
        else return false;
        node.editPermanent(node.permanentBase(), kind, quality);
        return true;
    }
    private static Aspect randomAspect(RandomSource random) {
        var choices = random.nextFloat() < .685f ? TCAspects.catalog().primals() : TCAspects.catalog().all();
        var list = new ArrayList<>(choices);
        return list.get(random.nextInt(list.size()));
    }
    private static List<List<AuraNodeBlockEntity>> nodes(ServerLevel level, BlockPos center, int radius) {
        var result = new ArrayList<List<AuraNodeBlockEntity>>();
        long squared = (long) radius * radius;
        for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; ++x)
            for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; ++z) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                var group = new ArrayList<AuraNodeBlockEntity>();
                for (var entity : chunk.getBlockEntities().values())
                    if (entity instanceof AuraNodeBlockEntity node && !node.isRemoved()
                            && node.getBlockPos().distSqr(center) <= squared) group.add(node);
                if (!group.isEmpty()) result.add(group);
            }
        return result;
    }
    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int n = values.size(); n > 1; --n) java.util.Collections.swap(values, n - 1, random.nextInt(n));
    }
}
