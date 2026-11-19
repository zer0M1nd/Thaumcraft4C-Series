package tc4revival.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.TCArcane;

public  class HoleEffects {
    private HoleEffects() {}
    public static void tick(Level level, BlockPos pos, boolean transition) {
        if (transition) ClientFocusEffects.blockSparkle(level, pos, 0x400040);
        // Every exposed edge of an opaque wall; two adjoining walls may each produce a sparkle, as in TC4.
        for (var wall : Direction.values()) {
            var adjacent = pos.relative(wall); var state = level.getBlockState(adjacent);
            if (!state.isSolidRender(level, adjacent)) continue;
            for (var open : Direction.values()) {
                if (open.getAxis() == wall.getAxis()) continue;
                var other = pos.relative(open); var otherState = level.getBlockState(other);
                if (otherState.isSolidRender(level, other) || otherState.is(TCArcane.PORTABLE_HOLE.get())) continue;
                double[] xyz = {.5, .5, .5};
                xyz[wall.getAxis().ordinal()] = wall.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
                xyz[open.getAxis().ordinal()] = open.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
                for (var axis : Direction.Axis.values()) if (axis != wall.getAxis() && axis != open.getAxis()) xyz[axis.ordinal()] = level.random.nextFloat();
                ClientFocusEffects.blueSparkle(level, Vec3.atLowerCornerOf(pos).add(xyz[0], xyz[1], xyz[2]));
            }
        }
    }
}
