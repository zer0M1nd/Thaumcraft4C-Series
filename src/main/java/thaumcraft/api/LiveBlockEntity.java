package thaumcraft.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Mutation guard: never loads a chunk and rejects detached, stale or off-thread instances. */
public class LiveBlockEntity {
    public static boolean server(BlockEntity entity) {
        return !entity.isRemoved() && entity.getLevel() instanceof ServerLevel level
                && level.getServer().isSameThread() && level.hasChunkAt(entity.getBlockPos())
                && level.getBlockEntity(entity.getBlockPos()) == entity;
    }
}
