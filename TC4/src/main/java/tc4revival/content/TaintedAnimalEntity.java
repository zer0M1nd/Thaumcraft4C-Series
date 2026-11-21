package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.IForgeShearable;

/** The old forms are hostile mobs, with none of the domestic animal interactions. */
public class TaintedAnimalEntity extends Monster implements TaintedMob, IForgeShearable {
    public enum Kind {
        CHICKEN(8,3,.4,2,.5f,.8f,"Chicken",22,0xc0c0c0),
        COW(40,6,.27,0,.9f,1.3f,"Cow",23,8272443),
        CREEPER(30,2,.25,0,.6f,1.8f,"Creeper",24,0x00ff00),
        PIG(20,4,.275,2,.9f,.9f,"Pig",25,0xef99ef),
        SHEEP(20,3,.25,1,.9f,1.3f,"Sheep",26,0x808080),
        VILLAGER(30,4,.3,0,.6f,1.8f,"Villager",27,0x00ffff);
        public final double health, damage, speed, armor;
        public final float width,height;
        public final String legacy;
        public final int eggMetadata,eggColor;
        Kind(double health,double damage,double speed,double armor,float width,float height,String legacy,int eggMetadata,int eggColor) {
            this.health=health;this.damage=damage;this.speed=speed;this.armor=armor;
            this.width=width;this.height=height;this.legacy="Tainted"+legacy;this.eggMetadata=eggMetadata;this.eggColor=eggColor;
        }
        public String id() { return "tainted_"+name().toLowerCase(Locale.ROOT); }
    }
    private static final EntityDataAccessor<Boolean> SHEARED=SynchedEntityData.defineId(TaintedAnimalEntity.class,EntityDataSerializers.BOOLEAN);
    private final Kind kind;
    private int grazingTicks;
    public float flap,oldFlap,wingSpread,oldWingSpread;
    private float flapSpeed=1;
    public TaintedAnimalEntity(EntityType<? extends TaintedAnimalEntity> type,Level level,Kind kind) {
        super(type,level);this.kind=kind;
        if(kind!=Kind.CHICKEN && kind!=Kind.CREEPER)setPathfindingMalus(BlockPathTypes.WATER,16);
        installGoals();
    }
    public Kind kind() { return kind; }
    public static AttributeSupplier.Builder attributes(Kind kind) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,kind.health)
                .add(Attributes.ATTACK_DAMAGE,kind.damage).add(Attributes.MOVEMENT_SPEED,kind.speed)
                .add(Attributes.ARMOR,kind.armor).add(Attributes.FOLLOW_RANGE,16);
    }
    // Mob invokes this from its constructor, before the selected form is available.
    @Override protected void registerGoals() {}
    private void installGoals() {
        if(kind==Kind.CREEPER)return;
        goalSelector.addGoal(0,new FloatGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        int playerPriority=kind==Kind.SHEEP?3:2;
        goalSelector.addGoal(playerPriority,new TaintMeleeGoal(this,Player.class,false));
        targetSelector.addGoal(playerPriority,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        if(kind==Kind.VILLAGER) {
            ((GroundPathNavigation)getNavigation()).setCanOpenDoors(true);
            goalSelector.addGoal(2,new TaintedVillageGoals.Shelter(this));
            goalSelector.addGoal(3,new TaintedVillageGoals.DoorRestriction(this));
            goalSelector.addGoal(4,new OpenDoorGoal(this,true));
            goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,1));
            goalSelector.addGoal(6,new MoveThroughVillageGoal(this,1,false,4,()->true));
            goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,3,1));
            goalSelector.addGoal(9,new LookAtPlayerGoal(this,Villager.class,5,.02f));
            goalSelector.addGoal(9,new RandomStrollGoal(this,1));
            goalSelector.addGoal(10,new LookAtPlayerGoal(this,LivingEntity.class,8));
            return;
        }
        goalSelector.addGoal(3,new TaintMeleeGoal(this,Villager.class,true));
        targetSelector.addGoal(kind==Kind.SHEEP?3:2,new NearestAttackableTargetGoal<>(this,Villager.class,0,false,false,null));
        if(kind==Kind.SHEEP)goalSelector.addGoal(2,new ConvertGrassGoal(this));
        else {
            int priority=kind==Kind.CHICKEN?3:8;
            goalSelector.addGoal(priority,new TaintMeleeGoal(this,Animal.class,kind==Kind.CHICKEN));
            targetSelector.addGoal(priority,new NearestAttackableTargetGoal<>(this,Animal.class,0,false,false,null));
        }
        if(kind==Kind.CHICKEN)goalSelector.addGoal(2,new LeapAtTargetGoal(this,.3f));
        int wander=kind==Kind.CHICKEN?3:kind==Kind.SHEEP?6:5;
        goalSelector.addGoal(wander,new RandomStrollGoal(this,1));
        goalSelector.addGoal(wander+1,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(wander+2,new RandomLookAroundGoal(this));
    }
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(SHEARED,false); }
    public boolean sheared() { return entityData.get(SHEARED); }
    @Override public boolean removeWhenFarAway(double distance) { return kind==Kind.COW; }
    @Override public float getVoicePitch() { return .7f; }
    @Override protected float getSoundVolume() { return kind==Kind.COW?.4f:super.getSoundVolume(); }
    @Override protected SoundEvent getAmbientSound() {
        return switch(kind) {case CHICKEN->SoundEvents.CHICKEN_AMBIENT;case COW->SoundEvents.COW_AMBIENT;
            case PIG->SoundEvents.PIG_AMBIENT;case SHEEP->SoundEvents.SHEEP_AMBIENT;case VILLAGER->SoundEvents.VILLAGER_AMBIENT;default->null;};
    }
    @Override protected SoundEvent getHurtSound(DamageSource damage) {
        return switch(kind) {case CHICKEN->SoundEvents.CHICKEN_HURT;case COW->SoundEvents.COW_HURT;
            case VILLAGER->SoundEvents.VILLAGER_HURT;case CREEPER->SoundEvents.CREEPER_HURT;default->getAmbientSound();};
    }
    @Override protected SoundEvent getDeathSound() {
        return switch(kind) {case PIG->SoundEvents.PIG_DEATH;case VILLAGER->SoundEvents.VILLAGER_DEATH;
            case CREEPER->SoundEvents.CREEPER_DEATH;default->getHurtSound(damageSources().generic());};
    }
    @Override protected void playStepSound(BlockPos pos,BlockState state) {
        var sound=switch(kind) {case COW->SoundEvents.COW_STEP;case PIG->SoundEvents.PIG_STEP;case SHEEP->SoundEvents.SHEEP_STEP;default->null;};
        if(sound!=null)playSound(sound,.15f,1);else super.playStepSound(pos,state);
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) {
        return kind!=Kind.CHICKEN && super.causeFallDamage(distance,multiplier,source);
    }
    @Override public void setLastHurtByMob(LivingEntity attacker) {
        super.setLastHurtByMob(attacker);
        if(kind==Kind.VILLAGER)TaintedVillageGoals.reportAttack(this,attacker);
    }
    @Override public void aiStep() {
        if(level().isClientSide && grazingTicks>0)grazingTicks--;
        super.aiStep();
        if(kind==Kind.CHICKEN) {
            oldFlap=flap;oldWingSpread=wingSpread;
            wingSpread=Mth.clamp(wingSpread+(onGround()?-.3f:1.2f),0,1);
            if(!onGround())flapSpeed=Math.max(1,flapSpeed);
            flapSpeed*=.9f;
            var velocity=getDeltaMovement();
            if(!onGround() && velocity.y<0)setDeltaMovement(velocity.multiply(1,.9,1));
            flap+=2*flapSpeed;
        }
        if(kind==Kind.VILLAGER && !level().isClientSide)TaintedVillageGoals.updateHome(this);
        if(level().isClientSide && tickCount<5)tc4revival.client.TaintedAnimalEffects.appear(this);
    }
    @Override public boolean isShearable(ItemStack tool,Level level,BlockPos pos) { return kind==Kind.SHEEP && !sheared(); }
    @Override public List<ItemStack> onSheared(Player player,ItemStack tool,Level level,BlockPos pos,int fortune) {
        if(!isShearable(tool,level,pos) || level.isClientSide)return List.of();
        entityData.set(SHEARED,true);
        var result=new ArrayList<ItemStack>();
        int count=1+random.nextInt(3);
        for(int i=0;i<count;i++)result.add(new ItemStack(Items.PURPLE_WOOL));
        return result;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);if(kind==Kind.SHEEP)tag.putBoolean("Sheared",sheared()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);entityData.set(SHEARED,tag.getBoolean("Sheared")); }
    @Override public void handleEntityEvent(byte event) {
        if(event==10 && kind==Kind.SHEEP)grazingTicks=40;else super.handleEntityEvent(event);
    }
    public float grazingHeadHeight(float partial) {
        if(grazingTicks<=0)return 0;
        if(grazingTicks<4)return (grazingTicks-partial)/4;
        if(grazingTicks<=36)return 1;
        return (40-grazingTicks+partial)/4;
    }
    public float grazingHeadAngle(float partial) {
        if(grazingTicks>4 && grazingTicks<=36)return .62831855f+.2199115f*Mth.sin((grazingTicks-4-partial)*28.7f/32);
        return grazingTicks>0?.62831855f:getXRot()*Mth.DEG_TO_RAD;
    }

    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    static  class ConvertGrassGoal extends Goal {
        private final TaintedAnimalEntity sheep;
        ConvertGrassGoal(TaintedAnimalEntity sheep) { this.sheep=sheep;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP)); }
        @Override public boolean canUse() {
            return sheep.random.nextInt(250)==0 && (sheep.level().getBlockState(sheep.blockPosition()).is(Blocks.GRASS)
                    || sheep.level().getBlockState(sheep.blockPosition().below()).is(Blocks.GRASS_BLOCK));
        }
        @Override public void start() { sheep.grazingTicks=40;sheep.level().broadcastEntityEvent(sheep,(byte)10);sheep.getNavigation().stop(); }
        @Override public boolean canContinueToUse() { return sheep.grazingTicks>0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { sheep.grazingTicks=0; }
        @Override public void tick() {
            if(--sheep.grazingTicks!=4 || !(sheep.level() instanceof ServerLevel level))return;
            var pos=sheep.blockPosition();var grass=level.getBlockState(pos);
            boolean atFeet=grass.is(Blocks.GRASS)||grass.is(Blocks.FERN)||grass.is(Blocks.DEAD_BUSH);
            if(!atFeet && !level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK))return;
            level.levelEvent(2001,atFeet?pos:pos.below(),net.minecraft.world.level.block.Block.getId(atFeet?Blocks.GRASS.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState()));
            level.setBlock(pos,TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),level,pos),3);
            WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND);
            sheep.entityData.set(SHEARED,false);
        }
    }
}
