package tc4revival.content;

import java.lang.reflect.Field;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** Elemental side strikes share the primary attack charge without recursively invoking the item callback. */
public  class ElementalSwordCombat {
    private static final Field FLOATING=ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class,"f_9737_");
    private ElementalSwordCombat() {}
    public static void resetFloating(ServerPlayer player) {
        if (player.connection==null) return;
        try { FLOATING.setInt(player.connection,0); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot reset elemental sword floating counter",exception); }
    }
    public static void attack(Player player,LivingEntity target) {
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target)) || !target.isAttackable() || target.skipAttackInteraction(player)) return;
        float damage=(float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float enchanted=EnchantmentHelper.getDamageBonus(player.getMainHandItem(),target.getMobType());
        float charge=player.getAttackStrengthScale(.5f);
        damage*=.2f+charge*charge*.8f; enchanted*=charge;
        int knockback=EnchantmentHelper.getKnockbackBonus(player)+(player.isSprinting()?1:0);
        if (damage<=0 && enchanted<=0) return;
        boolean critical=charge>.9f && !player.isSprinting() && player.fallDistance>0 && !player.onGround() && !player.onClimbable() && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger();
        var criticalEvent=net.minecraftforge.common.ForgeHooks.getCriticalHit(player,target,critical,critical?1.5f:1);
        critical=criticalEvent!=null;
        if (critical && damage>0) damage*=criticalEvent.getDamageModifier();
        damage+=enchanted;
        int fire=EnchantmentHelper.getFireAspect(player); boolean lit=fire>0 && !target.isOnFire();
        if (lit) target.setSecondsOnFire(1);
        boolean hit=target.hurt(player.damageSources().playerAttack(player),damage);
        if (hit) {
            if (knockback>0) {
                float radians=player.getYRot()*(float)Math.PI/180;
                target.push(-Mth.sin(radians)*knockback*.5f,.1,Mth.cos(radians)*knockback*.5f);
                var movement=player.getDeltaMovement(); player.setDeltaMovement(movement.x*.6,movement.y,movement.z*.6); player.setSprinting(false);
            }
            if (critical) player.crit(target);
            if (enchanted>0) player.magicCrit(target);
            player.setLastHurtMob(target); EnchantmentHelper.doPostHurtEffects(target,player);
        }
        if(hit){
            var held=player.getMainHandItem(); if (!held.isEmpty()) held.hurtEnemy(target,player);
            player.awardStat(Stats.DAMAGE_DEALT,Math.round(damage*10));
            EnchantmentHelper.doPostDamageEffects(player,target);
            player.causeFoodExhaustion(.1f);
        }
        if (fire>0 && hit) target.setSecondsOnFire(fire*4); else if (lit) target.clearFire();
    }
}
