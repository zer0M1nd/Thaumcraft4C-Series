package tc4revival.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Original guard toggles and order-upgrade gates, shared by target acquisition and attack goals. */
public  class GolemCombatTargets {
    private GolemCombatTargets() {}
    public static boolean valid(GolemWorkHost host,LivingEntity target) {
        if (target==host.mob() || !target.isAlive() || !host.mob().canAttack(target) || host.mob().isAlliedTo(target)) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (target instanceof Player player && player.getGameProfile().getName().equals(host.ownerName())) return false;
        double range=host.configuration().range();
        if (target.blockPosition().distSqr(host.home())>=range*range) return false;
        boolean animal=(target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature)
                && !(target instanceof Enemy) && !(target instanceof AbstractGolem)
                && !(target instanceof TamableAnimal tame && tame.isTame());
        var config=host.configuration();
        if (config.core()==GolemCoreType.BUTCHER) return animal && (!(target instanceof Animal creature) || !creature.isBaby());
        if (target instanceof Creeper) return !config.toggle(4) && config.upgrades(4)>0;
        if (target instanceof Enemy) return !config.toggle(1);
        if (animal) return !config.toggle(2) && config.upgrades(4)>0;
        return target instanceof Player && !config.toggle(3) && config.upgrades(4)>0;
    }
}
