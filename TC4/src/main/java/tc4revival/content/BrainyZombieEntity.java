package tc4revival.content;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BrainyZombieEntity extends Zombie {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> VILLAGER=net.minecraft.network.syncher.SynchedEntityData.defineId(BrainyZombieEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CURING=net.minecraft.network.syncher.SynchedEntityData.defineId(BrainyZombieEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private int cureTime=-1;
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(VILLAGER,false);entityData.define(CURING,false);}
    public boolean villager(){return entityData.get(VILLAGER);}
    public void setVillager(boolean value){entityData.set(VILLAGER,value);}
    public boolean curing(){return entityData.get(CURING);}
    private record Group(net.minecraft.world.entity.SpawnGroupData vanilla,boolean villager) implements net.minecraft.world.entity.SpawnGroupData {}
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor world,net.minecraft.world.DifficultyInstance difficulty,net.minecraft.world.entity.MobSpawnType reason,net.minecraft.world.entity.SpawnGroupData group,net.minecraft.nbt.CompoundTag tag){
        boolean flag=group instanceof Group previous?previous.villager:random.nextFloat()<.05f;
        var spawned=super.finalizeSpawn(world,difficulty,reason,group instanceof Group previous?previous.vanilla:group,tag);setVillager(flag);return new Group(spawned,flag);
    }
    @Override public net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(villager() && stack.is(Items.GOLDEN_APPLE) && hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)){
            if(!level().isClientSide){if(!player.getAbilities().instabuild)stack.shrink(1);startCure(3600+random.nextInt(2401));}
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }return super.mobInteract(player,hand);
    }
    void startCure(int ticks){cureTime=ticks;entityData.set(CURING,true);removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,ticks,Math.min(level().getDifficulty().getId()-1,0)));level().broadcastEntityEvent(this,(byte)16);}
    @Override public void handleEntityEvent(byte event){if(event==16)level().playLocalSound(getX()+.5,getY()+.5,getZ()+.5,net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE,getSoundSource(),1+random.nextFloat(),random.nextFloat()*.7f+.3f,false);else super.handleEntityEvent(event);}
    @Override public boolean removeWhenFarAway(double distance){return !curing() && super.removeWhenFarAway(distance);}
    @Override public void tick(){
        if(!level().isClientSide && curing() && isAlive()){
            cureTime-=cureSpeed();if(cureTime<=0 && level() instanceof net.minecraft.server.level.ServerLevel server){
                var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(server);
                if(villager!=null){villager.moveTo(getX(),getY(),getZ(),getYRot(),getXRot());villager.finalizeSpawn(server,server.getCurrentDifficultyAt(blockPosition()),net.minecraft.world.entity.MobSpawnType.CONVERSION,null,null);villager.setAge(isBaby()?-24000:0);villager.setPersistenceRequired();
                    if(server.addFreshEntity(villager)){villager.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,200));server.levelEvent(1027,blockPosition(),0);discard();return;}}
                cureTime=20;
            }
        }super.tick();
    }
    private int cureSpeed(){int speed=1;if(random.nextFloat()>=.01f)return speed;int found=0;
        for(int x=(int)getX()-4;x<(int)getX()+4 && found<14;x++)for(int y=(int)getY()-4;y<(int)getY()+4 && found<14;y++)for(int z=(int)getZ()-4;z<(int)getZ()+4 && found<14;z++){
            var pos=new net.minecraft.core.BlockPos(x,y,z);if(!level().hasChunkAt(pos))continue;var state=level().getBlockState(pos);
            if(state.is(net.minecraft.world.level.block.Blocks.IRON_BARS)||state.getBlock() instanceof net.minecraft.world.level.block.BedBlock){found++;if(random.nextFloat()<.3f)speed++;}
        }return speed;
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("IsVillager",villager());tag.putInt("ConversionTime",curing()?cureTime:-1);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);setVillager(tag.getBoolean("IsVillager"));cureTime=tag.contains("ConversionTime")?tag.getInt("ConversionTime"):-1;entityData.set(CURING,cureTime>=0);}

    public BrainyZombieEntity(EntityType<? extends BrainyZombieEntity> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return Zombie.createAttributes().add(Attributes.MAX_HEALTH,25).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0); }
    @Override protected void addBehaviourGoals() { super.addBehaviourGoals();targetSelector.addGoal(1,new HurtByTargetGoal(this)); }
    @Override public int getArmorValue() { return Math.min(20,super.getArmorValue()+3); }
    @Override protected boolean isSunSensitive(){return !isBaby();}
    @Override protected boolean convertsInWater() { return false; }
     // TC4 common and rare rolls are independent of vanilla's new table.
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override protected net.minecraft.world.item.ItemStack getSkull(){return net.minecraft.world.item.ItemStack.EMPTY;}

}
