package thaumicomniverse.compat.create.airship;

import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;

/** Endpoint constraints shared by the editor preview and authoritative graph edits. */
public final class FlightEditing {
    private FlightEditing() {}
    public static FlightCurve align(FlightCurve curve, Vec3 startDirection, Vec3 endDirection) {
        while (curve.controls.size() < 2) curve = curve.elevated();
        var points = new ArrayList<>(curve.controls);
        if (startDirection != null) points.set(0, curve.start.add(startDirection.normalize()
                .scale(Math.max(.001, points.get(0).distanceTo(curve.start)))));
        int last = points.size() - 1;
        if (endDirection != null) points.set(last, curve.end.subtract(endDirection.normalize()
                .scale(Math.max(.001, points.get(last).distanceTo(curve.end)))));
        return new FlightCurve(curve.start, points, curve.end);
    }
    /** Route locks constrain an axis, whereas junction locks retain an oriented tangent. */
    public static FlightCurve alignAxes(FlightCurve curve,Vec3 start,Vec3 end) {
        return align(curve,axisDirection(start,curve.tangent(0)),axisDirection(end,curve.tangent(curve.length())));
    }
    private static Vec3 axisDirection(Vec3 axis,Vec3 tangent) {
        return axis==null?null:axis.scale(axis.dot(tangent)<0?-1:1);
    }
    public static Vec3 direction(FlightGraph.Node node) {
        return node==null?null:Vec3.atLowerCornerOf(node.facing().getNormal());
    }
}
