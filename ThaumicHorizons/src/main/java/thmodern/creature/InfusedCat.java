package thmodern.creature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thmodern.Horizons;

public final class InfusedCat extends Cat {
    private static final EntityDataAccessor<Boolean> OCELOT=SynchedEntityData.defineId(InfusedCat.class,EntityDataSerializers.BOOLEAN);
    public InfusedCat(EntityType<? extends Cat> type,Level level){super(type,level);setTame(true);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(OCELOT,false);}
    public boolean panther(){return getType()==Horizons.GUARDIAN_PANTHER.get();}
    public boolean gravekeeper(){return getType()==Horizons.GRAVEKEEPER.get();}
    public boolean ocelotAppearance(){return entityData.get(OCELOT);}
    @Override public boolean isTame(){return true;}
    @Override protected void registerGoals(){
        if(getType()==Horizons.GUARDIAN_PANTHER.get()){
            goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new SitWhenOrderedToGoal(this));goalSelector.addGoal(3,new LeapAtTargetGoal(this,.5f));
            goalSelector.addGoal(4,new MeleeAttackGoal(this,1,true));goalSelector.addGoal(5,new FollowOwnerGoal(this,1,10,5,false));goalSelector.addGoal(6,new BreedGoal(this,.8));
            goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,10));
            targetSelector.addGoal(1,new OwnerHurtByTargetGoal(this));targetSelector.addGoal(2,new OwnerHurtTargetGoal(this));targetSelector.addGoal(3,new HurtByTargetGoal(this).setAlertOthers());
        }else{
            super.registerGoals();
            if(getType()==Horizons.GRAVEKEEPER.get()){
                targetSelector.removeAllGoals(goal->true);
                targetSelector.addGoal(2,new NearestAttackableTargetGoal<Mob>(this,Mob.class,0,false,false,entity->entity.getMobType()==MobType.UNDEAD||net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString().equals("thaumcraft:eldritch_guardian")));
            }
        }
    }
    @Override public boolean doHurtTarget(Entity target){
        if(gravekeeper()&&target instanceof LivingEntity living&&living.getMobType()==MobType.UNDEAD)return true;
        return target.hurt(damageSources().mobAttack(this),panther()?6:2);
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);var food=held.getFoodProperties(player);
        if(panther()&&food!=null&&getHealth()<getMaxHealth()){
            if(!level().isClientSide){heal(food.getNutrition());if(!player.getAbilities().instabuild)held.shrink(1);}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    public AABB auraBounds(){return new AABB(getX()-5,getY()-5,getZ()-5,getX()+5,getY()+5,getZ()+5);}
    @Override public void aiStep(){
        super.aiStep();if(!isAlive())return;
        if(level().isClientSide){if(getType()==Horizons.FAMILIAR.get()&&random.nextFloat()>.97f)thaumcraft.api.fx.LegacyEffects.sparkle(level(),new net.minecraft.world.phys.Vec3(getRandomX(.6),getRandomY(),getRandomZ(.6)),0xdf60df,2,1,0);return;}
        if(getType()==Horizons.FAMILIAR.get()&&tickCount%10==0)for(var player:level().getEntitiesOfClass(Player.class,auraBounds()))InfusedWolf.chargePlayer(player);
        if(gravekeeper())for(var entity:level().getEntitiesOfClass(LivingEntity.class,auraBounds(),e->e.isAlive()&&e.getMobType()==MobType.UNDEAD))entity.setSecondsOnFire(1);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("LegacyOcelot",ocelotAppearance());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(OCELOT,tag.getBoolean("LegacyOcelot"));setTame(true);}
}
