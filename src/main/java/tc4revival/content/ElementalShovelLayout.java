package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Shared placement and architect preview geometry from ItemElementalShovel. */
public  class ElementalShovelLayout {
    private ElementalShovelLayout() {}
    public static int orientation(ItemStack stack) { return stack.hasTag()?stack.getTag().getByte("or"):0; }
    public static void orientation(ItemStack stack,int value) { stack.getOrCreateTag().putByte("or",(byte)(value%3)); }
    public static List<BlockPos> plane(BlockPos center,Direction face,int orientation,float yaw) {
        var result=new ArrayList<BlockPos>(9); int quadrant=Mth.floor(yaw*4/360+.5)&3;
        for (int a=-1;a<=1;a++) for (int b=-1;b<=1;b++) {
            int x=0,y=0,z=0;
            if (orientation==1) {
                y=b;
                if (face.getAxis()==Direction.Axis.Y) { if (quadrant==0 || quadrant==2) x=a; else z=a; }
                else if (face.getAxis()==Direction.Axis.Z) z=a; else x=a;
            } else if (orientation==2) {
                if (face.getAxis()==Direction.Axis.Y) { y=b; if (quadrant==0 || quadrant==2) x=a; else z=a; }
                else { z=b; x=a; }
            } else if (face.getAxis()==Direction.Axis.Y) { x=a; z=b; }
            else if (face.getAxis()==Direction.Axis.Z) { x=a; y=b; }
            else { z=a; y=b; }
            result.add(center.offset(x,y,z));
        }
        return List.copyOf(result);
    }
    public static List<BlockPos> placement(Level level,BlockPos clicked,Direction face,int orientation,float yaw) {
        return plane(clicked.relative(face),face,orientation,yaw).stream().filter(pos->replaceable(level,pos)).toList();
    }
    public static boolean replaceable(Level level,BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        var state=level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.VINE) || state.is(Blocks.GRASS) || state.is(Blocks.FERN) || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.WATER) || state.canBeReplaced();
    }
}
