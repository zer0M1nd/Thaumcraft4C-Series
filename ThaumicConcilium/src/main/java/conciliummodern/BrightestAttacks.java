package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import thaumcraft.api.fx.LegacyEffects;

final class BrightestAttacks {
    static void continuous(BrightestOne boss,LivingEntity target){
        var level=boss.level();var random=boss.getRandom();
        switch(boss.aspectTag()){
            case "aer"->{var orb=new GolemOrbEntity(level,boss,target,false);orb.setPos(orb.position().add(boss.getLookAngle().x+(random.nextFloat()-.5)*5,.5,boss.getLookAngle().z+(random.nextFloat()-.5)*5));
                var direction=target.position().add(target.getDeltaMovement()).subtract(boss.position()).add(0,-target.getBbHeight()/2,0);orb.shoot(direction.x,direction.y,direction.z,.66f,5);level.addFreshEntity(orb);boss.playSound(TCSounds.ELDRITCH_ATTACK.get(),1,1+random.nextFloat()*.1f);}
            case "ignis"->{for(int i=0;i<30;i++){var ember=new EmberEntity(level,boss,15,25,0,20);var motion=ember.getDeltaMovement().multiply(random.nextDouble()-.5,random.nextDouble(),random.nextDouble()-.5);ember.setDeltaMovement(motion);ember.setPos(ember.position().add(motion.scale(8+random.nextDouble())));level.addFreshEntity(ember);}}
            case "tenebrae"->shadow(boss,target);
        }
    }
    static boolean attack(BrightestOne boss,LivingEntity target){
        var random=boss.getRandom();var level=boss.level();
        switch(boss.aspectTag()){
            case "aer"->{if(random.nextBoolean())zap(boss,target,25,false);else return true;}
            case "ignis"->{if(random.nextBoolean()){var look=target.getEyePosition().subtract(boss.getEyePosition()).normalize();for(int i=0;i<10;i++){
                var powder=new FireShardPowder(ConciliumEntities.FIRE_POWDER.get(),level);powder.setOwner(boss);powder.setPos(boss.getEyePosition().add(look).add(-Math.cos(Math.toRadians(boss.getYRot()))*.32,-.3,-Math.sin(Math.toRadians(boss.getYRot()))*.32));powder.setDeltaMovement(look.multiply(random.nextDouble(),1,random.nextDouble()));level.addFreshEntity(powder);}
            }else return true;}
            case "vacuos"->{if(random.nextBoolean()){
                if(level instanceof ServerLevel server && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,boss)){
                    var pos=target.blockPosition().below();if(level.getWorldBorder().isWithinBounds(pos))BrightestHoles.begin(boss,pos);
                }
            }else zap(boss,target,6.25f,true);}
            case "tenebrae"->{if(random.nextBoolean())return true;for(var player:level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,boss.getBoundingBox().inflate(32))){
                if(player.isSpectator() || player.getAbilities().instabuild)continue;var effect=new MobEffectInstance(MobEffects.BLINDNESS,600,30);effect.setCurativeItems(java.util.List.of());player.addEffect(effect);}}
        }
        return false;
    }
    private static void zap(BrightestOne boss,LivingEntity target,float damage,boolean voidDamage){boss.playSound(TCSounds.ZAP.get(),1,1.1f);tc4revival.network.FocusEffectPacket.lightning(boss.level(),boss,target);target.hurt(voidDamage?boss.damageSources().fellOutOfWorld():boss.damageSources().mobAttack(boss),damage);}
    /** Original 150 updates of a 0.15-block ray, with reflection and no gravity. */
    private static void shadow(BrightestOne boss,LivingEntity target){
        var level=boss.level();Vec3 step=target.getEyePosition().subtract(boss.getEyePosition()).normalize().scale(.15),pos=boss.getEyePosition();var points=new java.util.ArrayList<Vec3>();
        for(int i=0;i<150;i++){
            Vec3 end=pos.add(step);if(!level.hasChunkAt(BlockPos.containing(end)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(end)))break;
            var hit=level.clip(new ClipContext(pos,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss));
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos,end).inflate(.3),e->e!=boss && e.isAlive()))
                if(entity.getBoundingBox().inflate(.3).clip(pos,hit.getLocation()).isPresent())entity.hurt(boss.damageSources().indirectMagic(boss,boss),28);
            if(hit.getType()!=HitResult.Type.MISS){var normal=Vec3.atLowerCornerOf(hit.getDirection().getNormal());step=step.subtract(normal.scale(2*step.dot(normal)));pos=hit.getLocation().add(normal.scale(.001));}else pos=end;
            if(i>0)points.add(pos);
        }
        RuneFlow.shadow(boss,points);
    }
    private BrightestAttacks(){}
}
