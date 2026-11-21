package tc4revival.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** The same vanilla countdown governs real player hits and old summoned-mob loot credit. */
 class LegacyCombatCredit {
    private static final java.lang.reflect.Field RECENT_HIT=ObfuscationReflectionHelper.findField(LivingEntity.class,"f_20889_");
    private LegacyCombatCredit() {}
    static int remaining(LivingEntity entity) {
        try { return RECENT_HIT.getInt(entity); }
        catch(IllegalAccessException exception) { throw new IllegalStateException("Cannot read legacy player-hit eligibility",exception); }
    }
    static void set(LivingEntity entity,int ticks) {
        try { RECENT_HIT.setInt(entity,ticks); }
        catch(IllegalAccessException exception) { throw new IllegalStateException("Cannot preserve legacy player-hit eligibility",exception); }
    }
}
