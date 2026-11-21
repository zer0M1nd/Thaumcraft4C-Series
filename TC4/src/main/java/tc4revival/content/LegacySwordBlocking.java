package tc4revival.content;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Compatibility entry point; modern item use does not grant sword blocking. */
public  class LegacySwordBlocking {
    private LegacySwordBlocking() {}
    public static void hurt(LivingHurtEvent event) {
        // Kept as a source/binary compatibility entry point. Soap has no combat mitigation.
    }
}
