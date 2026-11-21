package tc4revival.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft")
public  class DeathGaze {
    private DeathGaze() {}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount%10!=0) return;
        var effect=player.getEffect(TCEffects.DEATH_GAZE.get()); if (effect==null) return;
        int range=Math.min(8+effect.getAmplifier()*3,24);
        for (var target:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(range))) {
            if (target==player || !target.isPickable() || !target.isAlive() || target.hasEffect(MobEffects.WITHER)
                    || target instanceof Player && !player.server.isPvpAllowed()) continue;
            var delta=new Vec3(target.getX(),target.getBoundingBox().minY+target.getBbHeight()/2f,target.getZ()).subtract(player.getEyePosition());
            if (!inCone(delta,player.getLookAngle(),range) || !player.hasLineOfSight(target)) continue;
            target.setLastHurtByMob(player); target.setLastHurtByPlayer(player);
            if (target instanceof PathfinderMob mob) mob.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,80));
        }
    }
    static boolean inCone(Vec3 delta,Vec3 look,int range) {
        double projection=delta.dot(look)/look.length();
        return projection/delta.length()>Math.cos(.75f/2f) && projection<range;
    }
}
