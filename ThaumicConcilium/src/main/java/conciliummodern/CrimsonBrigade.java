package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;

public final class CrimsonBrigade extends CultistEntity {
    public CrimsonBrigade(EntityType<? extends CrimsonBrigade> type,Level level){super(type,level);}
    public boolean paladin(){return getType()==CrimsonContent.PALADIN.get();}
    public boolean archer(){return getType()==CrimsonContent.ARCHER.get();}
    public static AttributeSupplier.Builder attributes(int health){return cultistAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,8);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        if(paladin()){
            goalSelector.addGoal(2,new PaladinRangedGoal());
            goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));
        }else goalSelector.addGoal(2,new RangedGoal());
        goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.6));
        goalSelector.addGoal(7,new RandomStrollGoal(this,1));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Thaumaturge.class,0,archer(),false,null){
            // Modern mustSee controls continuation only; original ranger/paladin acquisition also ignored sight.
            {if(!CrimsonBrigade.this.archer())targetConditions.ignoreLineOfSight();}
        });
    }
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,DifficultyInstance difficulty){
        setItemSlot(EquipmentSlot.FEET,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.BOOTS)));
        if(paladin()){
            var head=CrimsonContent.dyes()&&random.nextBoolean()?ConciliumDyesIntegration.equipment("paladin",EquipmentSlot.HEAD):ItemStack.EMPTY;
            setItemSlot(EquipmentSlot.HEAD,head.isEmpty()?new ItemStack(TCEldritch.armor(CultistEquipment.Armor.ROBE_HELMET)):head);
            setItemSlot(EquipmentSlot.CHEST,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.PLATE_CHEST)));
            setItemSlot(EquipmentSlot.LEGS,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.ROBE_LEGS)));
            var staff=CrimsonContent.item("thaumcraft:casting_wand");staff.getOrCreateTag().putString("rod","greatwood_staff");staff.getOrCreateTag().putString("cap","forbidden_magic:alchemical");setItemSlot(EquipmentSlot.MAINHAND,staff);
        }else{
            String role=archer()?"archer":"ranger";
            for(var slot:java.util.List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS))setItemSlot(slot,ConciliumDyesIntegration.equipment(role,slot));
            if(archer())setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        }
    }
    @Override protected void populateDefaultEquipmentEnchantments(net.minecraft.util.RandomSource random,DifficultyInstance difficulty){
        if(!paladin()&&!getMainHandItem().isEmpty()&&random.nextFloat()<.25f*difficulty.getSpecialMultiplier())
            setItemSlot(EquipmentSlot.MAINHAND,EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(5+difficulty.getSpecialMultiplier()*random.nextInt(18)),false));
    }
    @Override protected void customServerAiStep(){super.customServerAiStep();if(archer()&&tickCount%40==0)heal(1);}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return paladin()?ConciliumSounds.CHANT.get():super.getAmbientSound();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return paladin()?ConciliumSounds.GROWL.get():super.getDeathSound();}
    @Override protected float getSoundVolume(){return paladin()?.5f:super.getSoundVolume();}
    @Override public int getAmbientSoundInterval(){return paladin()?450:super.getAmbientSoundInterval();}
    @Override public float getVoicePitch(){return archer()?1.5f+(random.nextFloat()-random.nextFloat())*.2f:super.getVoicePitch();}
    // Original subclass dropFewItems also calls EntityCultist; the modern base delegates that roll to data.
    @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return TCEntities.CULTIST_KNIGHT.get().getDefaultLootTable();}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        int roll=random.nextInt(10);if(roll==0)spawnAtLocation(new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get()),1.5f);
        else if(roll==1)spawnAtLocation(new ItemStack(TCItems.material(MaterialType.VOID_SEED)),1.5f);
        else if(roll<=3+looting){
            if(!archer()&&!paladin()&&!random.nextBoolean())spawnAtLocation(PowderType.byId(random.nextInt(6)).shard().copyWithCount(1+random.nextInt(looting+3)),1.5f);
            else spawnAtLocation(new ItemStack(TCItems.material(MaterialType.GOLD_COIN),archer()?1+roll:1+random.nextInt(looting+3)),1.5f);
            if(archer()&&random.nextBoolean())spawnAtLocation(new ItemStack(TCItems.PRIMAL_ARROWS.get(random.nextInt(6)).get(),1+roll),1.5f);
        }
        super.dropCustomDeathLoot(source,looting,recentlyHit);
    }
    /** 1.7.10 arrow AI: 20 visible ticks before stopping, distance-based delay, no shot beyond 24 blocks. */
    private final class PaladinRangedGoal extends Goal {
        private LivingEntity target;
        private int cooldown=-1,seeTime;
        PaladinRangedGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public boolean canUse(){var next=getTarget();if(next==null||!next.isAlive())return false;target=next;return distanceToSqr(next.getX(),next.getBoundingBox().minY,next.getZ())>=64;}
        @Override public boolean canContinueToUse(){return canUse()||target!=null&&target.isAlive()&&!getNavigation().isDone();}
        @Override public void stop(){target=null;seeTime=0;cooldown=-1;}
        @Override public void tick(){
            if(target==null||!target.isAlive())return;
            double distance=distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());
            boolean visible=getSensing().hasLineOfSight(target);seeTime=visible?seeTime+1:0;
            if(distance<=576&&seeTime>=20)getNavigation().stop();else getNavigation().moveTo(target,1);
            getLookControl().setLookAt(target,30,30);
            if(--cooldown==0){
                if(distance>576||!visible)return;
                paladinAttack(target);cooldown=net.minecraft.util.Mth.floor((float)Math.sqrt(distance)/24*20+20);
            }else if(cooldown<0)cooldown=net.minecraft.util.Mth.floor((float)Math.sqrt(distance)/24*20+20);
        }
    }
    private final class RangedGoal extends Goal {
        private int cooldown,meleeCooldown,strafeTime,direction=1;
        RangedGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public boolean canUse(){var target=getTarget();return target!=null&&target.isAlive();}
        @Override public boolean canContinueToUse(){return canUse()&&distanceToSqr(getTarget())<=1000;}
        @Override public void start(){cooldown=0;meleeCooldown=0;}
        @Override public void stop(){cooldown=0;getNavigation().stop();}
        @Override public void tick(){
            var target=getTarget();if(target==null)return;
            double dx=getX()-target.getX(),dy=getY()-target.getY(),dz=getZ()-target.getZ();
            float yaw=net.minecraft.util.Mth.wrapDegrees((float)(Math.atan2(-dz,-dx)*180/Math.PI)-90);
            setYRot(yaw);yRotO=yaw;setYHeadRot(yaw);yBodyRot=yaw;
            boolean visible=getSensing().hasLineOfSight(target);double distance=dx*dx+dz*dz;
            Vec3 away=new Vec3(dx,0,dz).normalize();double vertical=onGround()?0:getDeltaMovement().y;
            if(archer()&&getMainHandItem().isEmpty()){
                setDeltaMovement(-away.x*.25,vertical,-away.z*.25);getNavigation().moveTo(target,.25);
                if(meleeCooldown>0)meleeCooldown--;
                if(distance<=4&&dy*dy<=4&&meleeCooldown<=0){swing(InteractionHand.MAIN_HAND);doHurtTarget(target);meleeCooldown=20;}
                return;
            }
            if(distance<340){if(strafeTime<=0){strafeTime=40;direction=random.nextBoolean()?1:-1;}else strafeTime--;
                Vec3 velocity=away.scale(.25).add(-away.z*.1*direction,vertical,away.x*.1*direction);setDeltaMovement(velocity);getNavigation().stop();
            }else getNavigation().moveTo(getX()+away.x*6,getY(),getZ()+away.z*6,.25);
            if(cooldown<20)cooldown++;if(cooldown>=20&&visible&&distance<=400){if(archer())arrow(target);else powder(target);cooldown=0;}
        }
    }
    private void paladinAttack(LivingEntity target){
        playSound(ConciliumSounds.SPELL.get(),1,1);swing(InteractionHand.MAIN_HAND);
        if(random.nextBoolean()){
            if(target instanceof Player){var shot=new EtherealShackles(this);var delta=target.position().add(target.getDeltaMovement()).subtract(position());shot.shoot(delta.x,target.getY()-getY(),delta.z,2,2);level().addFreshEntity(shot);playSound(ConciliumSounds.SHACKLES_THROW.get(),.5f,1);}
        }else{lookAt(target,100,100);level().addFreshEntity(new CrimsonOrb(this));}
    }
    private void arrow(LivingEntity target){
        var arrow=PrimalArrowEntity.create(this,random.nextInt(6));int power=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,getMainHandItem());
        arrow.setBaseDamage(2+random.nextInt(8)+(power>0?power*.5+.5:0));
        int punch=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,getMainHandItem());if(punch>0)arrow.setKnockback(punch);
        if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,getMainHandItem())>0)arrow.setSecondsOnFire(5);
        var delta=target.position().add(target.getDeltaMovement()).subtract(position());arrow.shoot(delta.x,target.getY()-getY(),delta.z,2.5f,1.5f);
        playSound(SoundEvents.SKELETON_SHOOT,1,1/(random.nextFloat()*.4f+.8f));level().addFreshEntity(arrow);
    }
    private void powder(LivingEntity target){
        playSound(TCSounds.ICE.get(),.3f,.8f+random.nextFloat()*.1f);Vec3 aim=getLookAngle();
        for(int i=0,count=4+random.nextInt(6);i<count;i++){
            var shot=new ShardPowder(this,PowderType.byId(random.nextInt(2)),getEyePosition().add(aim).add(-Math.cos(Math.toRadians(getYRot()))*.16,-.5,-Math.sin(Math.toRadians(getYRot()))*.16));
            shot.initialAge(20);
            shot.setDeltaMovement(aim.add(random.nextGaussian()*.09,random.nextGaussian()*.09,random.nextGaussian()*.09));level().addFreshEntity(shot);
        }
    }
}
