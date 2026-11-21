package conciliummodern;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

public final class MadThaumaturge extends Monster {
    private int ridingAttackDelay;
    public MadThaumaturge(EntityType<? extends MadThaumaturge> type,Level level){super(type,level);setCanPickUpLoot(false);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,40).add(Attributes.MOVEMENT_SPEED,.4).add(Attributes.ATTACK_DAMAGE,5);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,1));
        goalSelector.addGoal(6,new MoveThroughVillageGoal(this,1,false,4,()->false));
        goalSelector.addGoal(7,new RandomStrollGoal(this,1));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,CultistEntity.class,true));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Thaumaturge.class,true));
    }
    @Override public int getArmorValue(){return Math.min(20,super.getArmorValue()+2);}
    @Override public boolean canBeLeashed(Player player){return !isLeashed();}
    @Override public double getMyRidingOffset(){return isPassenger()?-1.5:0;}
    public void equip(){
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(TCItems.voidTool(random.nextBoolean()?VoidEquipment.Tool.HOE:VoidEquipment.Tool.SHOVEL)));
        setDropChance(EquipmentSlot.MAINHAND,.05f);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){
        var result=super.finalizeSpawn(level,difficulty,reason,group,tag);equip();return result;
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        if(ridingAttackDelay>0)ridingAttackDelay--;
        var attacker=getLastHurtByMob();
        if(!isPassenger() && attacker!=null && attacker.isAlive() && !attacker.isVehicle() && distanceToSqr(attacker)<4 && random.nextInt(100)>90)
            startRiding(attacker,true);
        if(isPassenger()){
            var vehicle=getVehicle();
            if(vehicle==null || !vehicle.isAlive()){stopRiding();return;}
            if(ridingAttackDelay==0){ridingAttackDelay=10+random.nextInt(10);doHurtTarget(vehicle);if(random.nextFloat()<.2f)stopRiding();}
        }
    }
    @Override protected SoundEvent getAmbientSound(){return ConciliumSounds.SAD.get();}
    @Override protected SoundEvent getDeathSound(){return ConciliumSounds.SAD.get();}
    @Override protected float getSoundVolume(){return .3f;}
    @Override public int getAmbientSoundInterval(){return 100;}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        if(random.nextInt(10)>7){
            int roll=random.nextInt(10);
            String key=roll<=3?"REFLECTIONFOCI":roll<=5?"BOTTLEOFTHICKTAINT":roll<=7?"RUNICWINDINGS":"DESTCRYSTAL";
            String hint=roll<=3?"REFLECTION_HINT":roll<=5?"THICK_TAINT_HINT":roll<=7?"WINDINGS_HINT":"DEST_CRYSTAL_HINT";
            var research=ResearchCatalogs.catalog().find(ThaumicConcilium.ID+":"+key);
            research.flatMap(definition->new ResearchNotes(TCAspects.catalog()).generate(definition,random::nextInt,tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get()))
                    .ifPresent(note->{var stack=ResearchNotesItem.create(note);stack.getOrCreateTag().putBoolean(hint,true);spawnAtLocation(stack,1.5f);});
        }else{
            int count=random.nextInt(2)+looting;
            if(count>0)spawnAtLocation(new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get(),count),1.5f);
        }
        super.dropCustomDeathLoot(source,looting,recentlyHit);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("RidingAttackDelay",ridingAttackDelay);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);ridingAttackDelay=Math.max(0,Math.min(19,tag.getInt("RidingAttackDelay")));}
}
