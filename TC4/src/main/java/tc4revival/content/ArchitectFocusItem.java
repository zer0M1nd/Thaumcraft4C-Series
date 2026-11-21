package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared G/shift-G settings and connected-block traversal for trade and warding. */
public abstract class ArchitectFocusItem extends WandFocusItem {
    public abstract int maximumArea(ItemStack focus);
    public abstract boolean volume();
    protected abstract boolean eligible(Level level, BlockPos origin, BlockPos candidate, Player player);
    public boolean architect(ItemStack wand) { return upgradeLevel(attached(wand), FocusUpgrade.ARCHITECT.id) > 0; }
    public int area(ItemStack wand, char axis) {
        int maximum = maximumArea(attached(wand)); String key = "area" + axis;
        return wand.hasTag() && wand.getTag().contains(key) ? Mth.clamp(wand.getTag().getInt(key), 0, maximum) : maximum;
    }
    public int dimension(ItemStack wand) { return wand.hasTag() ? Mth.clamp(wand.getTag().getInt("aread"), 0, volume() ? 3 : 2) : 0; }
    public void toggle(ItemStack wand, boolean shifted) {
        if (!architect(wand)) return;
        int dimension = dimension(wand), maximum = maximumArea(attached(wand));
        if (shifted) wand.getOrCreateTag().putInt("aread", (dimension + 1) % (volume() ? 4 : 3));
        else for (char axis : new char[]{'x', 'z', 'y'}) {
            if (dimension == 0 || dimension == 1 && axis == 'x' || dimension == 2 && axis == 'z' || dimension == 3 && axis == 'y')
                wand.getOrCreateTag().putInt("area" + axis, (area(wand, axis) + 1) % (maximum + 1));
        }
    }
    public List<BlockPos> blocks(ItemStack wand, Level level, BlockPos origin, Direction face, Player player) {
        int x = architect(wand) ? area(wand, 'x') : 0, y = architect(wand) ? area(wand, 'y') : 0, z = architect(wand) ? area(wand, 'z') : 0;
        return ArchitectSelection.collect(origin, face, x, y, z, volume(), pos -> !level.isOutsideBuildHeight(pos)
                && level.hasChunkAt(pos) && eligible(level, origin, pos, player));
    }
    public boolean showAxis(ItemStack wand, Direction face, Direction.Axis axis) {
        int dimension = dimension(wand);
        if (dimension == 0) return volume() || axis != face.getAxis();
        var selected = dimension == 3 ? face.getAxis() : dimension == 1 ? face.getAxis() == Direction.Axis.Y ? Direction.Axis.X : Direction.Axis.Y
                : face.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
        return axis == selected;
    }
}
