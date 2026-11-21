package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;

public  class CultistLeaderEntity extends ThaumcraftBossEntity implements CultistMob,RangedAttackMob {
    private static final EntityDataAccessor<Byte> TITLE=SynchedEntityData.defineId(CultistLeaderEntity.class,EntityDataSerializers.BYTE);
    private static final String[] TITLES={"Alberic","Anselm","Bastian","Beturian","Chabier","Chorache","Chuse","Dodorol","Ebardo","Ferrando","Fertus","Guillen","Larpe","Obano","Zelipe"};
    public CultistLeaderEntity(EntityType<? extends CultistLeaderEntity> type,Level level) { super(type,level); xpReward=40; }
    public static AttributeSupplier.Builder attributes() { return bossAttributes().add(Attributes.MAX_HEALTH,125).add(Attributes.MOVEMENT_SPEED,.32).add(Attributes.ATTACK_DAMAGE,5); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(TITLE,(byte)0); }
    @Override public void generateChampionName(ChampionMobs.Kind kind) { setCustomName(net.minecraft.network.chat.Component.translatable(getType().getDescriptionId(),TITLES[Byte.toUnsignedInt(entityData.get(TITLE))%TITLES.length],ChampionMobs.name(kind))); }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof CultistMob; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,30,40,24) {
            @Override public boolean canUse() { return super.canUse() && getTarget()!=null && getTarget().isAlive() && distanceToSqr(getTarget())>=256; }
        });
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false)); goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new RandomStrollGoal(this,.8)); goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8)); goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new CultistHurtGoal(this)); targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        for (var armor:new CultistEquipment.Armor[]{CultistEquipment.Armor.LEADER_HELMET,CultistEquipment.Armor.LEADER_CHEST,CultistEquipment.Armor.LEADER_LEGS,CultistEquipment.Armor.BOOTS})
            setItemSlot(armor.slot.getSlot(),new ItemStack(TCEldritch.armor(armor)));
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(world.getDifficulty()==Difficulty.EASY ? TCItems.voidTool(VoidEquipment.Tool.SWORD) : TCEldritch.CRIMSON_SWORD.get()));
        float factor=difficulty.getSpecialMultiplier();
        if (random.nextFloat()<.5f*factor) setItemSlot(EquipmentSlot.MAINHAND,EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(7+factor*random.nextInt(22)),false));
        entityData.set(TITLE,(byte)random.nextInt(TITLES.length)); return super.finalizeSpawn(world,difficulty,reason,group,tag);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        var area=new AABB(getX(),getY(),getZ(),getX(),getY(),getZ()).inflate(8);
        for (var ally:level().getEntitiesOfClass(CultistEntity.class,area)) if (!ally.hasEffect(MobEffects.REGENERATION)) ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60,1));
    }
    @Override public void performRangedAttack(LivingEntity target,float power) {
        if (level().isClientSide || !getSensing().hasLineOfSight(target)) return;
        swing(InteractionHand.MAIN_HAND); getLookControl().setLookAt(target.getX(),target.getY()+target.getBbHeight()/2,target.getZ(),30,30);
        var shot=new GolemOrbEntity(level(),this,target,true);
        shot.advanceCultistLaunch();
        shot.shoot(target.getX()-getX(),target.getY()+target.getBbHeight()/2-(getY()+getBbHeight()/2)+2,target.getZ()-getZ(),.66f,3);
        playSound(TCSounds.ELDRITCH_ATTACK.get(),1,1+random.nextFloat()*.1f); level().addFreshEntity(shot);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("title",entityData.get(TITLE)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(TITLE,(byte)(Byte.toUnsignedInt(tag.getByte("title"))%TITLES.length)); }
}
