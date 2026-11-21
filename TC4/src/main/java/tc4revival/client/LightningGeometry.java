package tc4revival.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

/** Immutable vector arithmetic replaces TC4's mutable vector/matrix helper library. */
 class LightningGeometry {
    private record Point(Vec3 base, Vec3 offset) { Vec3 position() { return base.add(offset); } }
    static  class Edge {
        final Point start, end;
        final float brightness;
        final int progress;
        Edge previous, next;
        Edge(Point start, Point end, float brightness, int progress) {
            this.start = start; this.end = end; this.brightness = brightness; this.progress = progress;
        }
        Vec3 start() { return start.position(); }
        Vec3 end() { return end.position(); }
        Vec3 direction() { return end().subtract(start()); }
        Vec3 join(boolean beginning, Vec3 view, float width) {
            var adjacent = beginning ? previous : next;
            var direction = direction().normalize();
            if (adjacent == null) return view.cross(direction).scale(width);
            var neighbor = adjacent.direction().normalize();
            var average = direction.add(neighbor).normalize();
            // The old renderer computes this angle after replacing the current tangent by the average.
            double sine = Math.sin(Math.acos(Math.max(-1, Math.min(1, average.dot(neighbor.scale(-1))))) / 2);
            return view.cross(average).scale(width / Math.max(.0001, sine));
        }
    }
    final Vec3 origin;
    final List<Edge> edges;
    final int travelTicks;
    final int lifetime;
    final int increment;
    final boolean nodeColor;
    int age;
    LightningGeometry(Vec3 from, Vec3 to, long seed) {
        this(from,to,seed,6,.5f,8);
    }
    LightningGeometry(Vec3 from, Vec3 to, long seed, int duration, float spread, int increment) {
        this(from,to,seed,duration,spread,increment,false);
    }
    LightningGeometry(Vec3 from, Vec3 to, long seed, int duration, float spread, int increment, boolean nodeColor) {
        this.nodeColor=nodeColor;
        this.increment=increment;
        origin = from;
        var random = new Random(seed);
        random.nextInt(3); // The legacy base constructor consumes this before the duration constructor.
        lifetime = duration + random.nextInt(duration) - duration / 2;
        double distance = from.distanceTo(to);
        travelTicks = (int) (distance * 3); age = -travelTicks;
        List<Edge> current = new ArrayList<>();
        current.add(new Edge(new Point(from, Vec3.ZERO), new Point(to, Vec3.ZERO), 1, 0));
        int[] divisors = {8, 12, 17, 23, 30, 34, 40};
        for (int pass = 0; pass < divisors.length; pass++) {
            var refined = new ArrayList<Edge>();
            float chance = pass == 0 ? .7f : pass < 4 ? .5f : 0;
            for (var old : current) {
                var axis = old.direction();
                var perpendicular = axis.z == 0 ? new Vec3(-axis.y, axis.x, 0) : new Vec3(0, axis.z, -axis.y);
                var offset = rotate(perpendicular, axis, random.nextFloat() * 360)
                        .scale((random.nextFloat() - .5f) * distance * spread / divisors[pass]);
                var middle = new Point(old.start().add(axis.scale(.5)), offset);
                var first = new Edge(old.start, middle, old.brightness, old.progress * 2);
                var second = new Edge(middle, old.end, old.brightness, old.progress * 2 + 1);
                first.previous = old.previous;
                if (first.previous != null) first.previous.next = first;
                first.next = second; second.previous = first;
                refined.add(first);
                if (random.nextFloat() < chance) {
                    var splitDirection = second.direction();
                    var splitAxis = rotate(new Vec3(0, splitDirection.z, -splitDirection.y), splitDirection, random.nextFloat() * 360);
                    var extension = rotate(splitDirection, splitAxis, (random.nextFloat() * .66f + .33f) * (45 + pass * 5)).scale(.1);
                    var branch = new Edge(middle, new Point(old.end.base, old.end.offset.add(extension)), old.brightness / 2, second.progress);
                    branch.previous = first; refined.add(branch);
                }
                refined.add(second);
                if (old.next != null) old.next.previous = second;
            }
            current = refined;
        }
        current.sort(Comparator.comparingDouble(edge -> -edge.brightness));
        edges = List.copyOf(current);
    }
    void tick() { age = Math.min(lifetime, age + increment); }
    boolean expired() { return age >= lifetime; }
    int visibleSegments(float partial) { return travelTicks == 0 ? 128 : (int) ((age + partial + travelTicks) * 128 / travelTicks); }
    private static Vec3 rotate(Vec3 vector, Vec3 axis, double degrees) {
        if (axis.lengthSqr() < 1e-12) return vector;
        var unit = axis.normalize(); double angle = Math.toRadians(degrees), cosine = Math.cos(angle), sine = Math.sin(angle);
        return vector.scale(cosine).add(unit.cross(vector).scale(sine)).add(unit.scale(unit.dot(vector) * (1 - cosine)));
    }
}
