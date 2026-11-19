package tc4revival.content;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;

/** Focus delays use simulation ticks, shared across a wielder's wands. */
public  class WandCooldowns {
    // Entity.equals uses the network ID, shared by the two logical sides in singleplayer.
    private static final Map<LivingEntity, Long> ENDS = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private WandCooldowns() {}
    public static void clear(LivingEntity entity) { ENDS.remove(entity); }
    public static boolean begin(LivingEntity entity, int milliseconds) {
        synchronized (ENDS) {
            long now = entity.level().getGameTime();
            if (ENDS.getOrDefault(entity, 0L) > now) return false;
            if (milliseconds <= 0) ENDS.remove(entity); else ENDS.put(entity, now + (milliseconds+49L)/50);
            return true;
        }
    }
    public static float secondsRemaining(LivingEntity entity) {
        return Math.max(0, ENDS.getOrDefault(entity, 0L) - entity.level().getGameTime()) / 20f;
    }
}
