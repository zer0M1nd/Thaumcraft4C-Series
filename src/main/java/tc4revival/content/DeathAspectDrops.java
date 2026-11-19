package tc4revival.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import tc4revival.aspect.*;
import tc4revival.aspect.entity.EntityAspects;

/** The old death hook rolls once per reduced primal, independent of the mob-loot gamerule. */
 class DeathAspectDrops {
    private DeathAspectDrops() {}
    static void drop(LivingEntity victim) {
        if(!(victim.level() instanceof ServerLevel level) || LegacyCombatCredit.remaining(victim)<=0)return;
        var found=EntityAspects.resolve(victim);if(found.isEmpty())return;
        var primals=TCAspects.catalog().toPrimals(found.get().aspects(),AspectCatalog.Reduction.SUM);
        for(var entry:primals.view().entrySet()) {
            int available=entry.getValue();
            if(available<=0 || !level.random.nextBoolean())continue;
            var orb=new AspectOrbEntity(level,victim.getX(),victim.getY(),victim.getZ(),entry.getKey(),1+level.random.nextInt(available));
            thaumcraft.api.aspects.AspectDropHandlers.spawn(level,victim,orb);
        }
    }
}
