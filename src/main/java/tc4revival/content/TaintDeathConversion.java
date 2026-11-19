package tc4revival.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Taint poison converts on death from any cause, not just the poison damage tick. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class TaintDeathConversion {
    private TaintDeathConversion() {}
    @SubscribeEvent public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if(!event.getLevel().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.animal.IronGolem golem)
            golem.targetSelector.addGoal(1,new TaintedVillageGoals.Defend(golem));
    }
    @SubscribeEvent public static void died(LivingDeathEvent event) {
        var original=event.getEntity();
        if(!(original.level() instanceof ServerLevel level))return;
        // These are mutually exclusive branches in the original death hook.
        if(original instanceof TaintedMob || !original.hasEffect(TCEffects.FLUX_TAINT.get())) { DeathAspectDrops.drop(original);return; }
        var kind=original instanceof Creeper?TaintedAnimalEntity.Kind.CREEPER:
                original instanceof Sheep?TaintedAnimalEntity.Kind.SHEEP:
                original instanceof Cow?TaintedAnimalEntity.Kind.COW:
                original instanceof Pig?TaintedAnimalEntity.Kind.PIG:
                original instanceof Chicken?TaintedAnimalEntity.Kind.CHICKEN:
                original instanceof Villager?TaintedAnimalEntity.Kind.VILLAGER:null;
        Mob converted;
        if(kind!=null)converted=TCEntities.taintedAnimal(kind).create(level);
        else {
            var slime=TCEntities.THAUMIC_SLIME.get().create(level);
            if(slime==null)return;
            slime.setSlimeSize((int)(1+Math.min(original.getMaxHealth()/10,6)));converted=slime;
        }
        if(converted==null)return;
        converted.moveTo(original.getX(),original.getY(),original.getZ(),original.getYRot(),0);
        // Do not cancel the death event: the original drops and player respawn must still run.
        // Players complete the modern death lifecycle instead of being removed from the level early.
        if(level.addFreshEntity(converted) && !(original instanceof Player))original.discard();
    }
}
