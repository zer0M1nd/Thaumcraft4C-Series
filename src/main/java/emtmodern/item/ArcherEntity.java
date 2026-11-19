package emtmodern.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The original hammer guardian is a vanilla snow golem with arrows, not a bow-wielding custom model. */
@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD)
public class ArcherEntity extends SnowGolem {
    public ArcherEntity(EntityType<? extends SnowGolem> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return SnowGolem.createAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.MOVEMENT_SPEED,.3);}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){event.put(emtmodern.ElectroMagicTools.ARCHER_ENTITY.get(),attributes().build());}
    @Override protected void registerGoals(){
        super.registerGoals();
        goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,6));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Mob.class,0,true,false,target->target instanceof Enemy));
        goalSelector.addGoal(4,new RandomStrollGoal(this,.3));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
        var result=super.finalizeSpawn(world,difficulty,reason,data,tag);setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));return result;
    }
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor){
        var arrow=new Arrow(level(),this);double dx=target.getX()-getX(),dz=target.getZ()-getZ(),horizontal=Math.sqrt(dx*dx+dz*dz);
        double dy=target.getEyeY()-.7-arrow.getY();
        if(horizontal>1.0e-7){arrow.setPos(getX()+dx/horizontal,arrow.getY(),getZ()+dz/horizontal);arrow.shoot(dx,dy+horizontal*.2,dz,1.6f,14-level().getDifficulty().getId()*4);}
        int power=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,getMainHandItem()),punch=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,getMainHandItem());
        arrow.setBaseDamage(distanceFactor*2+getRandom().nextGaussian()*.25+level().getDifficulty().getId()*.11+(power>0?power*.5+.5:0));
        if(punch>0)arrow.setKnockback(punch);if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,getMainHandItem())>0)arrow.setSecondsOnFire(100);
        playSound(SoundEvents.ARROW_SHOOT,1,1/(getRandom().nextFloat()*.4f+.8f));level().addFreshEntity(arrow);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        int arrows=random.nextInt(3+looting),bones=random.nextInt(3+looting);
        for(int i=0;i<arrows;i++)spawnAtLocation(Items.ARROW);for(int i=0;i<bones;i++)spawnAtLocation(Items.BONE);
        // 1.7.10's rare-drop branch used rand.nextInt(200)-looting < 5.
        if(recentlyHit&&random.nextInt(200)-looting<5)spawnAtLocation(Items.SNOWBALL);
    }
    @Override public void die(DamageSource source){
        super.die(source);if(!level().isClientSide)level().explode(this,getX(),getY(),getZ(),1,false,Level.ExplosionInteraction.BLOCK);discard();
    }
}
