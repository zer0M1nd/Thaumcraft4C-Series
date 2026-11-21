package tc4revival.content;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** TC4 targets a padded entity hitbox and separately requires sight of its eyes. */
public  class FocusTargeting {
    private FocusTargeting() {}
    public static boolean visible(Entity source, Vec3 destination) {
        return source.level().clip(new ClipContext(source.getEyePosition(), destination,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source)).getType() == HitResult.Type.MISS;
    }
    public static Entity pointed(Entity source, double minimum, double range, double padding) {
        return pointed(source, minimum, range, padding, entity -> true);
    }
    public static Entity pointed(Entity source, double minimum, double range, double padding, java.util.function.Predicate<Entity> filter) {
        var start = source.getEyePosition();
        var direction = source.getLookAngle().scale(range);
        var end = start.add(direction);
        Entity selected = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (var entity : source.level().getEntities(source, source.getBoundingBox().expandTowards(direction).inflate(padding))) {
            var wielder=thaumcraft.api.wands.WandActors.resolve(source);
            if (wielder!=source && (entity==wielder || wielder.isAlliedTo(entity))) continue;
            if (!filter.test(entity) || !entity.isPickable() || source.distanceTo(entity) < minimum || !visible(source, entity.getEyePosition())) continue;
            var bounds = entity.getBoundingBox().inflate(Math.max(.8, entity.getPickRadius()));
            var intersection = bounds.clip(start, end);
            if (bounds.contains(start)) { nearest = 0; selected = entity; break; }
            if (intersection.isEmpty()) continue;
            double distance = start.distanceTo(intersection.get());
            if (distance < nearest) { nearest = distance; selected = entity; }
        }
        return selected;
    }
}
