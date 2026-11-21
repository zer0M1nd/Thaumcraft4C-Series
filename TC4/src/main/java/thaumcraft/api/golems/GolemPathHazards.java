package thaumcraft.api.golems;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Additional contact hazards shared by pathfinding and safe home recovery. Register during setup. */
public final class GolemPathHazards {
    private static final Set<Block> REGISTERED=ConcurrentHashMap.newKeySet();
    private GolemPathHazards() {}
    public static void register(Block block) { REGISTERED.add(java.util.Objects.requireNonNull(block)); }
    public static boolean contains(BlockState state) {
        var block=state.getBlock();
        return state.is(tc4revival.content.GolemDoorNavigation.AVOID)||REGISTERED.contains(block)
                ||block instanceof CactusBlock||block instanceof SweetBerryBushBlock||block instanceof WitherRoseBlock
                ||block instanceof BaseFireBlock||block instanceof MagmaBlock||block instanceof PointedDripstoneBlock;
    }
}
