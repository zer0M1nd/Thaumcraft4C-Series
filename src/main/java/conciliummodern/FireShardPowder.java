package conciliummodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Retains the existing fire-powder registry ID used by the brightest one and saved worlds. */
public final class FireShardPowder extends ShardPowder {
    public FireShardPowder(EntityType<? extends FireShardPowder> type,Level level){super(type,level);powderType(PowderType.FIRE);}
}
