package heartwork.compat.blade;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.*;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.Vec3;

public final class HeartworkArts {
    public static final String[] NAMES={"sakuya","spear2","saxianshizhan","sabailou1","saxianshizhan2","sabailou2",
            "saxianshizhan4","saxianshizhan3","sabailou3","iai","slashdimension_old","sakuya2","iai_speed"};
    public static void register() {
        for(String name:NAMES) {
            var id=BladeCompat.id(name);
            boolean draw=name.startsWith("iai")||name.equals("saxianshizhan3"),dimension=name.equals("sabailou3")||name.equals("saxianshizhan2")||name.equals("slashdimension_old");
            BladeCompat.COMBOS.register(name,() -> ComboState.Builder.newInstance().startAndEnd(draw||dimension?1923:2000,draw||dimension?1928:2019).speed(draw?0.5F:1F).priority(20)
                    .next(e -> ComboStateRegistry.NONE.getId()).nextOfTimeout(e -> ComboStateRegistry.NONE.getId())
                    .clickAction(e -> perform(name,e)).build());
            // The callback retains the old per-art fallback durability and no-target cost.
            BladeCompat.ARTS.register(name,() -> new SlashArts(e -> id).setComboStateSuper(e -> id).setProudSoulCost(0));
        }
    }
    public static Entity findTarget(LivingEntity user,ISlashBladeState state) {
        Entity locked=state.getTargetEntity(user.level());
        if(validTarget(user,locked) && user.distanceTo(locked)<30) return locked;
        for(int distance=2;distance<20;distance+=2) {
            var bounds=user.getBoundingBox().inflate(2,0.25,2).move(user.getLookAngle().normalize().scale(distance));
            Entity closest=null;float best=30;
            for(Entity candidate:user.level().getEntities(user,bounds,e -> validTarget(user,e))) {
                float length=user.distanceTo(candidate);
                if(length<best) {closest=candidate;best=length;}
            }
            if(closest!=null)return closest;
        }
        return null;
    }
    public static boolean validTarget(LivingEntity user,Entity target) {
        return target instanceof LivingEntity living && living.isAlive() && target!=user && !target.isSpectator()
                && !user.isAlliedTo(target) && (!(user instanceof Player player) || !(target instanceof Player other) || player.canHarmPlayer(other));
    }
    public static void deflect(Entity user,net.minecraft.world.entity.projectile.Projectile projectile,float damage) {
        if(projectile.getOwner()==user)return;
        if(projectile instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile && user instanceof LivingEntity living &&
                projectile.hurt(user.damageSources().mobAttack(living),damage))return;
        projectile.discard();
    }
    public static void spearTick(LivingEntity user,int age) {
        var box=new net.minecraft.world.phys.AABB(user.position().subtract(1.5,1.5,1.5),user.position().add(1.5,1.5,1.5));
        for(var projectile:user.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,box,p -> p.getOwner()!=user))deflect(user,projectile,1);
        if(age%2==0 && user.getMainHandItem().getItem() instanceof ItemSlashBlade)
            for(Entity victim:user.level().getEntities(user,box,e -> validTarget(user,e)))melee(user,victim);
    }
    private static void pay(LivingEntity user,ItemStack stack,ISlashBladeState state,int soul,int durability) {
        if(state.getProudSoulCount()>=soul)state.setProudSoulCount(state.getProudSoulCount()-soul);
        else stack.hurtAndBreak(durability,user,ItemSlashBlade.getOnBroken(stack));
    }
    private static void effect(LivingEntity user,MobEffect effect,int ticks,int amplifier) { user.addEffect(new MobEffectInstance(effect,ticks,amplifier,true,true)); }
    private static void rush(LivingEntity user,double speed) {
        double yaw=Math.toRadians(user.getYRot());user.setDeltaMovement(-Math.sin(yaw)*speed,user.getDeltaMovement().y,Math.cos(yaw)*speed);user.hurtMarked=true;
    }
    private static void teleport(LivingEntity user,Entity target,boolean targetY) {
        user.teleportTo(target.getX(),targetY?target.getY():user.getY(),target.getZ());user.hurtMarked=true;
    }
    private static void melee(LivingEntity user,Entity target) { AttackManager.doMeleeAttack(user,target,true,true); }
    private static void launch(Entity target) { target.setDeltaMovement(0,0.5,0);target.hurtMarked=true; }
    public static void perform(String name,LivingEntity user) {
        if(!(user.level() instanceof ServerLevel world))return;
        ItemStack stack=user.getMainHandItem();
        var optional=stack.getCapability(ItemSlashBlade.BLADESTATE).resolve();if(optional.isEmpty())return;
        var state=optional.get();Entity target=findTarget(user,state);
        if(!name.startsWith("sabailou") && !name.equals("spear2") && target==null) {
            if(name.startsWith("iai")||name.startsWith("sakuya"))world.playSound(null,user.blockPosition(),SoundEvents.BLAZE_HURT,SoundSource.PLAYERS,1,1);
            return;
        }
        int cost=switch(name) {case "sabailou1" -> 30;case "sabailou2" -> 60;case "sabailou3" -> 100;case "iai" ->25;case "iai_speed","saxianshizhan4" ->50;default ->20;};
        int wear=switch(name) {case "sabailou2","iai_speed" ->20;case "sabailou3" ->stack.getMaxDamage()+1;case "saxianshizhan4" ->25;
            case "saxianshizhan","saxianshizhan2","saxianshizhan3" ->15;default ->10;};
        pay(user,stack,state,cost,wear);
        int level=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING,stack);
        float amplifier=state.getAttackAmplifier();
        switch(name) {
            case "sabailou1" -> effect(user,MobEffects.MOVEMENT_SPEED,1200,1);
            case "sabailou2" -> {effect(user,MobEffects.MOVEMENT_SPEED,1200,2);effect(user,MobEffects.BLINDNESS,100,3);effect(user,MobEffects.DAMAGE_BOOST,1200,1);}
            case "sabailou3" -> {effect(user,MobEffects.REGENERATION,80,3);effect(user,MobEffects.DAMAGE_BOOST,80,1);effect(user,MobEffects.WATER_BREATHING,80,3);
                effect(user,MobEffects.NIGHT_VISION,80,3);effect(user,MobEffects.HEAL,10,4);}
            case "spear2" -> {
                rush(user,user.onGround()?3.5:3.5*0.35);
                user.getPersistentData().putInt("HeartworkSpearTicks",7);
            }
            case "sakuya","sakuya2" -> {
                float damage=5+amplifier*(level/3F);boolean enhanced=name.equals("sakuya2");
                if(user.onGround())swords(user,target,damage,enhanced?8:6,state.getColorCode());
                if(!user.onGround() || enhanced)drives(user,damage,enhanced?4:2,enhanced?45:90,2,20,state.getColorCode());
                if(enhanced && !user.onGround())for(int i=0;i<2;i++)cut(user,target.position().add(0,target.getBbHeight()/2,0),damage+3,20,0x3333FF);
            }
            case "iai","iai_speed" -> {
                drives(user,2+amplifier*(level/2F),4,45,name.equals("iai")?0.3F:2F,20,state.getColorCode());
                if(name.equals("iai_speed"))effect(user,MobEffects.MOVEMENT_SPEED,1200,4);
            }
            case "saxianshizhan" -> {
                if(user.onGround()) {teleport(user,target,true);rush(user,4.5);cut(user,target.position().add(0,target.getBbHeight()/2,0),6,2,16770273);melee(user,target);launch(target);}
            }
            case "saxianshizhan2" -> {
                if(user.onGround()) {teleport(user,target,false);rush(user,2.5);}
                for(Entity victim:world.getEntities(user,target.getBoundingBox().inflate(2,0.25,2),e -> validTarget(user,e)))melee(user,victim);
                for(int i=0;i<4;i++) {float offset=user.getRandom().nextFloat()+user.getRandom().nextInt(4);
                    cut(user,target.position().add(offset,target.getBbHeight()/2,offset),1+amplifier*(level/5F),20,16770273);}
                rush(user,-2.5);
            }
            case "saxianshizhan3" -> {
                teleport(user,target,false);
                if(!user.onGround()) {user.setDeltaMovement(user.getDeltaMovement().multiply(1,0,1).add(0,-2.5,0));melee(user,target);}
                cut(user,target.position().add(0,target.getBbHeight()/2,0),6,2,16770273);melee(user,target);launch(target);
            }
            case "saxianshizhan4" -> {
                if(user.onGround()) {rush(user,12.5);swords(user,target,4+amplifier*(level/3F),10,state.getColorCode());
                    cut(user,target.position().add(0,target.getBbHeight()/2,0),6,2,16770273);melee(user,target);launch(target);}
            }
            case "slashdimension_old" -> {
                for(Entity victim:world.getEntities(user,target.getBoundingBox().inflate(2,0.25,2),e -> validTarget(user,e)))melee(user,victim);
                cut(user,target.position().add(0,target.getBbHeight()/2,0),0.5F+amplifier*(level/5F),1,0xFFFFFF);
                world.sendParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+target.getBbHeight(),target.getZ(),3,1,1,1,0);
                world.playSound(null,user.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,0.5F,1);
            }
        }
        world.sendParticles(ParticleTypes.SWEEP_ATTACK,user.getX(),user.getY()+1,user.getZ(),1,0,0,0,0);
        world.playSound(null,user.blockPosition(),name.startsWith("sakuya")||name.equals("spear2")?SoundEvents.GENERIC_EXPLODE:SoundEvents.BLAZE_HURT,SoundSource.PLAYERS,1,1);
    }
    private static void drives(LivingEntity user,float damage,int count,int angle,float speed,int lifetime,int color) {
        for(int i=0;i<count;i++) {
            var drive=new HeartworkDriveEntity(BladeCompat.DRIVE.get(),user.level());drive.setShooter(user);drive.setDamage(damage);drive.setColor(color);
            drive.moveTo(user.getX(),user.getY()+user.getEyeHeight()/2,user.getZ(),user.getYRot(),user.getXRot());
            drive.setRotationRoll(i*angle);drive.setSpeed(speed);drive.setLifetime(lifetime);
            var direction=user.getLookAngle();drive.shoot(direction.x,direction.y,direction.z,speed,0);user.level().addFreshEntity(drive);
        }
    }
    private static void swords(LivingEntity user,Entity target,float damage,int count,int color) {
        for(int i=0;i<count;i++) {
            var sword=new HeartworkSummonedSword(BladeCompat.SWORDS.get(),user.level());sword.setShooter(user);sword.setDamage(damage);sword.setColor(color);
            sword.configure(target,9+i*2,i);user.level().addFreshEntity(sword);
        }
    }
    private static void cut(LivingEntity user,Vec3 pos,float damage,int lifetime,int color) {
        var cut=new HeartworkCutEntity(BladeCompat.CUT.get(),user.level());cut.setShooter(user);cut.setPos(pos);cut.setDamage(damage);cut.setLifetime(lifetime);cut.setColor(color);user.level().addFreshEntity(cut);
    }
}
