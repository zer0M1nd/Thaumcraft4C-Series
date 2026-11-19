package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;

/** A possessed zombie is a carrier: its death immediately releases an armored crab. */
public  class InhabitedZombieEntity extends Zombie {
    public InhabitedZombieEntity(EntityType<? extends InhabitedZombieEntity> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return Zombie.createAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0); }
    @Override protected void addBehaviourGoals() {
        super.addBehaviourGoals(); targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,CultistEntity.class,0,true,false,null));
    }
    @Override public boolean killedEntity(ServerLevel level,LivingEntity target) { return true; }
    @Override protected boolean convertsInWater() { return false; }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        float chance=world.getDifficulty()==Difficulty.HARD ? .9f : .6f;
        setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.PLATE_HELMET)));
        if (random.nextFloat()<=chance) setItemSlot(EquipmentSlot.CHEST,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.PLATE_CHEST)));
        if (random.nextFloat()<=chance) setItemSlot(EquipmentSlot.LEGS,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.PLATE_LEGS)));
        return group;
    }
    @Override public void die(DamageSource source) {} // TC4 bypasses equipment, ordinary loot, and the vanilla death sequence.
    @Override protected void tickDeath() {
        if (level() instanceof ServerLevel server) {
            var crab=TCEntities.ELDRITCH_CRAB.get().create(server);
            if (crab!=null) { crab.moveTo(getX(),getEyeY(),getZ(),getYRot(),getXRot()); crab.setHelm(true); server.addFreshEntity(crab); }
            if (lastHurtByPlayerTime>0 && server.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) ExperienceOrb.award(server,position(),getExperienceReward());
            for (int index=0;index<20;index++) server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                    getX()+random.nextFloat()*getBbWidth()*2-getBbWidth(),getY()+random.nextFloat()*getBbHeight(),getZ()+random.nextFloat()*getBbWidth()*2-getBbWidth(),
                    0,random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02,1);
            discard();
        }
    }
    @Override public boolean checkSpawnRules(LevelAccessor world,MobSpawnType reason) {
        var area=new net.minecraft.world.phys.AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(32,16,32);
        return level().getEntitiesOfClass(InhabitedZombieEntity.class,area).isEmpty() && super.checkSpawnRules(world,reason);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return TCSounds.CRAB_TALK.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return net.minecraft.sounds.SoundEvents.HOSTILE_HURT; }
}
