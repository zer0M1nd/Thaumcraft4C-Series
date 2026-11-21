package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import tc4revival.content.*;

public final class CrimsonPontifex extends ThaumcraftBossEntity implements CultistMob {
    private static final EntityDataAccessor<Integer> TITLE=SynchedEntityData.defineId(CrimsonPontifex.class,EntityDataSerializers.INT);
    private static final String[] TITLES={"Ivius","Ufarihm","Ihith","Pemonar","Shagron","Ugimaex","Qroleus","Oxon","Rheforn","Zubras"};
    private int attackCounter,cultists,spellAggroCooldown;
    private java.util.UUID spellTargetId;
    private LivingEntity spellTarget;
    public CrimsonPontifex(EntityType<? extends CrimsonPontifex> type,Level level){super(type,level);xpReward=40;}
    public static AttributeSupplier.Builder attributes(){return bossAttributes().add(Attributes.MAX_HEALTH,150).add(Attributes.MOVEMENT_SPEED,.32).add(Attributes.ATTACK_DAMAGE,25);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TITLE,0);}
    @Override public void generateChampionName(ChampionMobs.Kind kind){setCustomName(Component.translatable(getType().getDescriptionId()+".titled",TITLES[entityData.get(TITLE)],ChampionMobs.name(kind)));}
    @Override public boolean isAlliedTo(Entity other){return other instanceof CultistMob;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,.7,false));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new CultistHurtGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,false,false,null));
    }
    public void equip(){
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(CrimsonContent.HAMMER.get()));setItemSlot(EquipmentSlot.HEAD,new ItemStack(CrimsonContent.HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST,new ItemStack(CrimsonContent.CHEST.get()));setItemSlot(EquipmentSlot.LEGS,new ItemStack(CrimsonContent.LEGS.get()));setItemSlot(EquipmentSlot.FEET,new ItemStack(CrimsonContent.BOOTS.get()));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){
        equip();float factor=difficulty.getSpecialMultiplier();if(random.nextFloat()<.5f*factor)setItemSlot(EquipmentSlot.MAINHAND,EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(7+factor*random.nextInt(22)),false));
        entityData.set(TITLE,random.nextInt(TITLES.length));return super.finalizeSpawn(world,difficulty,reason,group,tag);
    }
    public int cultists(){return cultists;}
    private java.util.List<CultistEntity> allies(){return level().getEntitiesOfClass(CultistEntity.class,new net.minecraft.world.phys.AABB(position(),position()).inflate(8),LivingEntity::isAlive);}
    private void spellTarget(LivingEntity target){spellTarget=target;spellTargetId=target==null?null:target.getUUID();}
    private LivingEntity spellTarget(){
        if(spellTarget==null&&spellTargetId!=null&&level() instanceof ServerLevel server&&server.getEntity(spellTargetId) instanceof LivingEntity living)spellTarget=living;
        if(spellTarget!=null&&(!spellTarget.isAlive()||spellTarget.isRemoved()||spellTarget.level()!=level()||isAlliedTo(spellTarget)||!canAttack(spellTarget)))spellTarget(null);
        return spellTarget;
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source))return false;cultists=allies().size();
        LivingEntity attacker=source.getEntity() instanceof LivingEntity living?living:source.getDirectEntity() instanceof LivingEntity direct?direct:null;
        if(attacker!=null&&!isAlliedTo(attacker)&&canAttack(attacker)){spellTarget(attacker);spellAggroCooldown=200;}
        return super.hurt(source,amount/(cultists+3));
    }
    @Override protected void customServerAiStep(){
        super.customServerAiStep();var allies=allies();cultists=allies.size();
        for(var ally:allies)if(!ally.hasEffect(MobEffects.REGENERATION))ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60,1));
        var target=spellTarget();--spellAggroCooldown;
        // Retain the original random re-selection window independently of melee/boss aggro.
        if(random.nextInt(10)>7&&(target==null||spellAggroCooldown--<=0)){
            target=level().getNearestPlayer(net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(32).ignoreLineOfSight(),this);
            spellTarget(target);if(target!=null)spellAggroCooldown=50;
        }
        if(target==null||distanceToSqr(target)>=256)return;
        if(!getSensing().hasLineOfSight(target)){if(attackCounter>0)attackCounter--;return;}
        if(++attackCounter<20)return;
        attackCounter=-20+random.nextInt(20);
        if(!(target instanceof ServerPlayer player))return;
        switch(random.nextInt(4)){
            case 0->{
                if(level().getEntitiesOfClass(WarpCharge.class,player.getBoundingBox().inflate(7),charge->charge.ownedBy(player)).isEmpty()){
                    var charge=ConciliumEntities.WARP_CHARGE.get().create(level());if(charge!=null){charge.initialize(player,1,0,false,false,true);charge.setPos(player.position().add((random.nextFloat()-.5)*4,0,(random.nextFloat()-.5)*4));level().addFreshEntity(charge);}
                }
            }
            case 1->CrimsonControl.enslave(player,this,11-attackCounter);
            case 2->summonPortal();
            case 3->{
                for(var victim:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(6),e->e!=this&&e.isAlive()&&!e.isSpectator()&&!(e instanceof CultistMob))){
                    if(victim instanceof Player p&&p.isCreative())continue;
                    if(victim.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE))){heal(10);CrimsonDrainNetwork.drain(this,victim);}
                }
            }
        }
    }
    private void summonPortal(){
        if(!(level() instanceof ServerLevel level)||level.getEntitiesOfClass(LesserPortal.class,getBoundingBox().inflate(64),LivingEntity::isAlive).size()>=3)return;
        var position=blockPosition().offset(random.nextInt(5)-random.nextInt(5),0,random.nextInt(5)-random.nextInt(5));
        if(position.getX()==blockPosition().getX()||position.getZ()==blockPosition().getZ()||!level.hasChunkAt(position)||!level.getWorldBorder().isWithinBounds(position)||!level.isEmptyBlock(position))return;
        var portal=CrimsonContent.PORTAL.get().create(level);if(portal==null)return;portal.setPos(position.getX()+.5,getY(),position.getZ()+.5);
        if(!level.noCollision(portal)||!level.addFreshEntity(portal))return;
        tc4revival.network.FocusEffectPacket.blockArc(level,this,position,false);playSound(TCSounds.WAND_FAIL.get(),1,1);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return ConciliumSounds.CHANT.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return ConciliumSounds.GROWL.get();}
    @Override protected float getSoundVolume(){return .5f;}
    @Override public int getAmbientSoundInterval(){return 450;}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        spawnAtLocation(new ItemStack(TCItems.lootBag(LootBagItem.Kind.RARE)),1.5f);
        int roll=random.nextInt(10);String key=roll<=3?"MATERIALPEELER":roll<=5?"PONTIFEXHAMMER":roll<=8?"MATERIALPEELER":"PONTIFEXROBE";
        // Witchery's lost spell branch is enabled only if its modern integration actually registers it.
        if(roll<=3&&thaumcraft.api.research.ResearchCategories.enabled("thaumicconcilium:CRIMSONSPELLS")&&tc4revival.research.ResearchCatalogs.catalog().find("thaumicconcilium:CRIMSONSPELLS").isPresent())key="CRIMSONSPELLS";
        tc4revival.research.ResearchCatalogs.catalog().find(ThaumicConcilium.ID+":"+key)
            .flatMap(research->new tc4revival.research.ResearchNotes(tc4revival.aspect.TCAspects.catalog()).generate(research,random::nextInt,tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get()))
            .ifPresent(note->spawnAtLocation(ResearchNotesItem.create(note),1.5f));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("title",entityData.get(TITLE));tag.putInt("AttackCounter",attackCounter);tag.putInt("SpellAggroCooldown",spellAggroCooldown);if(spellTargetId!=null)tag.putUUID("SpellTarget",spellTargetId);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TITLE,Math.floorMod(tag.getInt("title"),TITLES.length));attackCounter=net.minecraft.util.Mth.clamp(tag.getInt("AttackCounter"),-20,20);spellAggroCooldown=net.minecraft.util.Mth.clamp(tag.getInt("SpellAggroCooldown"),-200,200);spellTargetId=tag.hasUUID("SpellTarget")?tag.getUUID("SpellTarget"):null;spellTarget=null;}
}
