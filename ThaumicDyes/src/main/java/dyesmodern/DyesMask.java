package dyesmodern;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Both gaze paths are authoritative. No client packet can nominate a victim or another player. */
@Mod.EventBusSubscriber(modid=ThaumicDyes.ID)
public final class DyesMask {
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumicdyes:wither_mask"));
    private static final Map<ServerPlayer,Watch> WATCHING=new WeakHashMap<>();
    private record Watch(int target,int ticks){}
    private DyesMask(){}
    public static boolean wearing(Player player){return player.getItemBySlot(EquipmentSlot.HEAD).is(DyesItems.get("itemHelmetMaskTX"));}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!wearing(player)){WATCHING.remove(player);return;}
        if(player.tickCount%20==0)deathGaze(player);
        if(!wearing(player)){WATCHING.remove(player);return;}
        var target=pointed(player);if(target==null){WATCHING.remove(player);return;}
        var previous=WATCHING.get(player);int ticks=previous!=null&&previous.target()==target.getId()?previous.ticks()+1:1;
        WATCHING.put(player,new Watch(target.getId(),ticks));if(ticks%5==0)attackGaze(player,target);
    }
    public static boolean canHarm(ServerPlayer player,LivingEntity target){return target!=player&&target.isAlive()&&(! (target instanceof Player other)||player.server.isPvpAllowed()&&player.canHarmPlayer(other))&&player.hasLineOfSight(target);}
    public static boolean withinCone(Vec3 eye,Vec3 look,Vec3 center){var delta=center.subtract(eye);double along=delta.dot(look),length=delta.length();return length>0&&along<24&&along/length>Math.cos(.75/2);}
    public static void deathGaze(ServerPlayer player){
        if(!wearing(player))return;
        for(var target:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(24),entity->canHarm(player,entity))){
            if(!wearing(player))break;
            if(!withinCone(player.getEyePosition(),player.getLookAngle(),target.getBoundingBox().getCenter()))continue;
            if(MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target)))continue;
            target.setLastHurtByMob(player);target.setLastHurtByPlayer(player);if(target instanceof Mob mob)mob.setTarget(player);
            damageMask(player);
            switch(player.getRandom().nextInt(3)){
                case 0->target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,6));
                case 1->target.addEffect(new MobEffectInstance(MobEffects.WITHER,100,1));
                case 2->target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,3));
            }
        }
    }
    public static LivingEntity pointed(ServerPlayer player){
        Vec3 eye=player.getEyePosition(),direction=player.getLookAngle().scale(24),end=eye.add(direction);
        var block=player.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));double closest=block.getLocation().distanceToSqr(eye);
        Entity selected=null;
        for(var entity:player.level().getEntities(player,player.getBoundingBox().expandTowards(direction).inflate(1),Entity::isPickable)){
            var bounds=entity.getBoundingBox().inflate(entity.getPickRadius());var hit=bounds.clip(eye,end);double distance=bounds.contains(eye)?0:hit.map(eye::distanceToSqr).orElse(Double.POSITIVE_INFINITY);
            if(distance<closest&&(entity!=player.getVehicle()||entity.canRiderInteract())){selected=entity;closest=distance;}
        }
        return selected instanceof LivingEntity target&&canHarm(player,target)?target:null;
    }
    public static boolean attackGaze(ServerPlayer player,LivingEntity target){
        if(!wearing(player)||!canHarm(player,target)||pointed(player)!=target||MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target)))return false;
        damageMask(player);target.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE),player),1);
        DyesNetwork.maskBeam(player,target);return true;
    }
    private static void damageMask(ServerPlayer player){player.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(1,player,entity->entity.broadcastBreakEvent(EquipmentSlot.HEAD));}
}
