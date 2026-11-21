package thmodern.vat;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class CreatureInfusionEvents {
    public static float charge(LivingEntity entity){float value=entity.getPersistentData().getCompound(CreatureInfusions.DATA).getFloat("RunicCharge");return Float.isFinite(value)?Math.max(0,Math.min(6,value)):0;}
    private static void charge(LivingEntity entity,float amount){var data=entity.getPersistentData().getCompound(CreatureInfusions.DATA);data.putFloat("RunicCharge",Math.max(0,Math.min(6,amount)));entity.getPersistentData().put(CreatureInfusions.DATA,data);}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        var entity=event.getEntity();if(!(entity.level() instanceof ServerLevel level)||!(entity instanceof Mob mob))return;
        if(CreatureInfusions.has(entity,"runic_hide")&&entity.tickCount%100==0)charge(entity,charge(entity)+1);
        if(CreatureInfusions.has(entity,"shock_skin")&&entity.tickCount%30==0){
            var target=entity.getLastHurtByMob();if(target==null||!target.isAlive())target=mob.getTarget();
            if(target!=null&&target.isAlive()&&target.level()==level){
                target.hurt(entity.damageSources().indirectMagic(entity,entity),1);
                tc4revival.network.FocusEffectPacket.lightning(level,entity.getBoundingBox().getCenter(),target.getBoundingBox().getCenter());
                entity.playSound(tc4revival.content.TCSounds.ZAP.get(),1,1+(entity.getRandom().nextFloat()-entity.getRandom().nextFloat())*.2f);
            }
        }
    }
    public static boolean teleport(LivingEntity entity){
        if(!(entity.level() instanceof ServerLevel level))return false;var random=entity.getRandom();
        double x=entity.getX()+(random.nextDouble()-.5)*64,y=entity.getY()+random.nextInt(64)-32,z=entity.getZ()+(random.nextDouble()-.5)*64;
        var event=new net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity(entity,x,y,z);
        if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))return false;
        var pos=net.minecraft.core.BlockPos.containing(event.getTargetX(),event.getTargetY(),event.getTargetZ());
        if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        var before=entity.position();boolean moved=entity.randomTeleport(event.getTargetX(),event.getTargetY(),event.getTargetZ(),true);
        if(moved){level.playSound(null,before.x,before.y,before.z,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.NEUTRAL,1,1);entity.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,1,1);}return moved;
    }
    @SubscribeEvent public static void attack(LivingAttackEvent event){
        var entity=event.getEntity();if(entity.level().isClientSide)return;
        if(CreatureInfusions.has(entity,"ender_heart")&&event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)){event.setCanceled(true);teleport(entity);}
    }
    @SubscribeEvent public static void poison(LivingDamageEvent event){
        if(event.getEntity().level().isClientSide||event.getAmount()<=0)return;
        if(event.getSource().getDirectEntity() instanceof LivingEntity attacker&&CreatureInfusions.has(attacker,"eldritch_fangs"))event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON,40,0));
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void shield(LivingHurtEvent event){
        var entity=event.getEntity();var source=event.getSource();
        if(!(entity.level() instanceof ServerLevel level)||!CreatureInfusions.has(entity,"runic_hide")||source.is(DamageTypes.DROWN)||source.is(DamageTypes.WITHER)||source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.STARVE)||event.getAmount()<=0)return;
        float absorbed=Math.min(charge(entity),event.getAmount());if(absorbed<=0)return;charge(entity,charge(entity)-absorbed);event.setAmount(event.getAmount()-absorbed);
        int direction=source.is(DamageTypes.FALL)?-2:source.is(DamageTypes.FALLING_BLOCK)?-3:source.getEntity()!=null?source.getEntity().getId():-1;
        tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(()->entity),new tc4revival.network.RunicHitPacket(entity.getId(),direction));
        entity.playSound(tc4revival.content.TCSounds.RUNIC_SHIELD_EFFECT.get(),1,1);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof Mob mob))return;
        if(CreatureInfusions.has(mob,"portability")&&event.getItemStack().getItem() instanceof tc4revival.content.CastingWandItem){
            event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            if(event.getEntity() instanceof ServerPlayer player&&player.mayBuild()&&player.level().mayInteract(player,mob.blockPosition())){
                var stack=thmodern.containment.CreatureJar.bottle(mob);if(stack.isEmpty())return;
                var drop=new net.minecraft.world.entity.item.ItemEntity(mob.level(),mob.getX(),mob.getY()+1,mob.getZ(),stack);
                if(mob.level().addFreshEntity(drop)){if(mob.isLeashed())mob.dropLeash(true,true);mob.discard();}
            }
        }else if(CreatureInfusions.has(mob,"instilled_loyalty")&&mob.getPersistentData().getCompound(CreatureInfusions.DATA).hasUUID("Owner")&&mob.getPersistentData().getCompound(CreatureInfusions.DATA).getUUID("Owner").equals(event.getEntity().getUUID())){
            event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            if(!event.getLevel().isClientSide){var data=mob.getPersistentData().getCompound(CreatureInfusions.DATA);data.putBoolean("Sitting",!data.getBoolean("Sitting"));mob.setTarget(null);mob.getNavigation().stop();event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable(data.getBoolean("Sitting")?"thaumichorizons.loyal.wait":"thaumichorizons.loyal.follow",mob.getName()),true);}
        }
    }
}
