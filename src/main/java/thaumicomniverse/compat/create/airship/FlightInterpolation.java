package thaumicomniverse.compat.create.airship;

import net.minecraft.world.phys.Vec3;

/** Bounds network catch-up without limiting the configured flight speed. */
final class FlightInterpolation {
    private FlightInterpolation() {}
    static Vec3 next(Vec3 position,Vec3 target,int remaining,double speed) {
        var step=target.subtract(position).scale(1d/Math.max(1,remaining));
        double limit=Math.max(1,Math.abs(speed)*1.25);
        if(step.lengthSqr()>limit*limit)step=step.normalize().scale(limit);
        return position.add(step);
    }
}
