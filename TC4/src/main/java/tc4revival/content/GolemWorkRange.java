package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Legacy work distances use the entity position and the target block center. */
public class GolemWorkRange {
    public static boolean reaches(GolemWorkHost host,BlockPos pos) {
        return reaches(host,pos,4);
    }
    public static boolean reaches(GolemWorkHost host,BlockPos pos,double squaredLimit) {
        double reach=Math.sqrt(squaredLimit)+tc4revival.config.TCConfig.SERVER.golemInteractionBonus.get();
        return host.mob().distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=reach*reach;
    }
    public static boolean adjacent(GolemWorkHost host,BlockPos pos) {
        return reaches(host,pos,4);
    }
    public static double distanceSquared(AABB a,AABB b) {
        double x=Math.max(0,Math.max(a.minX-b.maxX,b.minX-a.maxX));
        double y=Math.max(0,Math.max(a.minY-b.maxY,b.minY-a.maxY));
        double z=Math.max(0,Math.max(a.minZ-b.maxZ,b.minZ-a.maxZ));
        return x*x+y*y+z*z;
    }
    private GolemWorkRange(){}
}
