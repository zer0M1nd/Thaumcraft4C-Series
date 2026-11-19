package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
public final class ThaumGib extends ConciliumHostile {
 private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(ThaumGib.class,EntityDataSerializers.INT);
 public ThaumGib(EntityType<? extends ThaumGib> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.85).add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,3);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,0);}
 public int gibType(){return entityData.get(TYPE);}public void setGibType(int type){entityData.set(TYPE,net.minecraft.util.Mth.clamp(type,0,5));}
 @Override protected void registerGoals(){basicGoals(.6,true);}
 @Override protected void targets(){legacyTargets(false,false,false,false);}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return RemainingMobs.GARGLE_ONE.get();}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return RemainingMobs.GARGLE_PAIN.get();}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return RemainingMobs.GARGLE_PAIN.get();}
 @Override protected float getSoundVolume(){return .6f;}
 @Override public int getAmbientSoundInterval(){return 65;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){int count=random.nextInt(2)+looting;int roll=random.nextInt(10);var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+(roll<4?"beef":roll<7?"chicken":"pork")+"_nugget"));if(count>0&&item!=null&&item!=net.minecraft.world.item.Items.AIR)spawnAtLocation(new ItemStack(item,count));super.dropCustomDeathLoot(source,looting,hit);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Type",gibType());}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setGibType(tag.getInt("Type"));}
}
