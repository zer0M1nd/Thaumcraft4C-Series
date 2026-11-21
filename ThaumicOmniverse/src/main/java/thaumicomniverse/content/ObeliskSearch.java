package thaumicomniverse.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import tc4revival.content.EldritchRingStructure;

/** Incremental nearest-first locate. Only candidate structure-start chunks are requested. */
public final class ObeliskSearch {
    private record Region(StructurePlacement placement, int x, int z, double distance) {}
    private record Key(StructurePlacement placement, int x, int z) {}
    public final BlockPos origin;
    private final int boss;
    private final ServerLevel level;
    private final PriorityQueue<Region> frontier = new PriorityQueue<>(Comparator.comparingDouble(Region::distance));
    private final Set<Key> visited = new HashSet<>();
    private final Set<Long> checkedChunks = new HashSet<>();
    private BlockPos best;
    private double bestDistance = Double.POSITIVE_INFINITY;
    private boolean complete;

    public ObeliskSearch(ServerLevel level, BlockPos origin, int boss) {
        this.level = level; this.origin = origin.immutable(); this.boss = boss;
        if (level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource
                || !tc4revival.config.TCConfig.SERVER.generates(tc4revival.config.TCConfig.WorldFeature.STRUCTURES)) {
            complete = true;
            return;
        }
        var state = level.getChunkSource().getGeneratorState();
        for (var set : state.possibleStructureSets()) {
            if (set.value().structures().stream().noneMatch(entry -> entry.structure().value() instanceof EldritchRingStructure)) continue;
            if (set.value().placement() instanceof RandomSpreadStructurePlacement placement) {
                add(placement, Math.floorDiv(origin.getX() >> 4, placement.spacing()), Math.floorDiv(origin.getZ() >> 4, placement.spacing()));
            } else if (set.value().placement() instanceof ConcentricRingsStructurePlacement placement) {
                var positions = state.getRingPositionsFor(placement);
                if (positions != null) for (var pos : positions) add(placement, pos.x, pos.z);
            } else {
                add(set.value().placement(), origin.getX() >> 4, origin.getZ() >> 4);
            }
        }
        complete = frontier.isEmpty();
    }
    private void add(StructurePlacement placement, int x, int z) {
        if (!visited.add(new Key(placement, x, z))) return;
        long size = (placement instanceof RandomSpreadStructurePlacement random ? random.spacing() : 1) * 16L, minX = x * size, minZ = z * size;
        var border = level.getWorldBorder();
        if (minX > border.getMaxX() || minX + size < border.getMinX() || minZ > border.getMaxZ() || minZ + size < border.getMinZ()) return;
        double dx = Math.max(0, Math.max(minX - origin.getX(), origin.getX() - (minX + size)));
        double dz = Math.max(0, Math.max(minZ - origin.getZ(), origin.getZ() - (minZ + size)));
        frontier.add(new Region(placement, x, z, dx * dx + dz * dz));
    }
    public void step() {
        if (complete) return;
        if (frontier.isEmpty() || frontier.peek().distance() > bestDistance) { complete = true; return; }
        var region = frontier.remove();
        if (!(region.placement() instanceof ConcentricRingsStructurePlacement)) {
            add(region.placement(), region.x() + 1, region.z()); add(region.placement(), region.x() - 1, region.z());
            add(region.placement(), region.x(), region.z() + 1); add(region.placement(), region.x(), region.z() - 1);
        }
        ChunkPos candidate = region.placement() instanceof RandomSpreadStructurePlacement random
                ? random.getPotentialStructureChunk(level.getSeed(), region.x() * random.spacing(), region.z() * random.spacing())
                : new ChunkPos(region.x(), region.z());
        if (!region.placement().isStructureChunk(level.getChunkSource().getGeneratorState(), candidate.x, candidate.z)) return;
        if (!checkedChunks.add(candidate.toLong()) || !level.getWorldBorder().isWithinBounds(candidate.getMiddleBlockPosition(64))
                || ObeliskClaims.get(level.getServer()).conquered(candidate.x, candidate.z)) return;
        var chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
        for (var start : chunk.getAllStarts().values()) {
            if (!start.isValid() || !(start.getStructure() instanceof EldritchRingStructure)) continue;
            for (var piece : start.getPieces()) {
                if (!(piece instanceof EldritchRingStructure.Piece)) continue;
                var tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
                if (boss >= 0 && tag.getInt("Boss") != boss) continue;
                BlockPos target = BlockPos.of(tag.getLong("Ground")).above();
                double dx = target.getX() - (double) origin.getX(), dz = target.getZ() - (double) origin.getZ();
                double distance = dx * dx + dz * dz;
                if (distance < bestDistance) { best = target; bestDistance = distance; }
            }
        }
    }
    public boolean complete() { return complete; }
    public BlockPos result() { return best; }
}
