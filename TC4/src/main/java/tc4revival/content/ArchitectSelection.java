package tc4revival.content;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Bounded connected selection shared by the server operation and its client preview. */
public  class ArchitectSelection {
    private ArchitectSelection() {}
    public static List<BlockPos> collect(BlockPos origin, Direction face, int x, int y, int z, boolean volume, Predicate<BlockPos> eligible) {
        if (x < 0 || y < 0 || z < 0 || x > 13 || y > 13 || z > 13) throw new IllegalArgumentException("Architect radius outside focus limits");
        int limitX = face.getAxis() == Direction.Axis.Z ? z : face.getAxis() == Direction.Axis.X ? y : x;
        int limitY = face.getAxis() == Direction.Axis.Y ? y : x;
        int limitZ = face.getAxis() == Direction.Axis.Z ? y : z;
        var result = new ArrayList<BlockPos>(); var visited = new HashSet<BlockPos>(); var pending = new ArrayDeque<BlockPos>(); pending.push(origin.immutable());
        var directions = Direction.values();
        while (!pending.isEmpty()) {
            var pos = pending.pop(); if (!visited.add(pos)) continue;
            if (Math.abs((long) pos.getX() - origin.getX()) > limitX || Math.abs((long) pos.getY() - origin.getY()) > limitY
                    || Math.abs((long) pos.getZ() - origin.getZ()) > limitZ || !eligible.test(pos)) continue;
            result.add(pos);
            for (int i = directions.length - 1; i >= 0; i--)
                if (volume || directions[i].getAxis() != face.getAxis()) pending.push(pos.relative(directions[i]));
        }
        return result;
    }
}
