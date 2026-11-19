package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class CultistEntity extends Monster implements CultistMob {
    protected CultistEntity(EntityType<? extends CultistEntity> type,Level level) {
        super(type,level); xpReward=10;
        if (getNavigation() instanceof GroundPathNavigation navigation) navigation.setCanOpenDoors(true);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER,8);
    }
    public static AttributeSupplier.Builder cultistAttributes() { return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,4); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this)); goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(5,new OpenDoorGoal(this,true)); goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new RandomStrollGoal(this,.8)); goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8)); goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new CultistHurtGoal(this)); targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
    }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return true; }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof CultistMob; }
    @Override protected void populateDefaultEquipmentEnchantments(net.minecraft.util.RandomSource random,DifficultyInstance difficulty) {}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        populateDefaultEquipmentSlots(random,difficulty); populateDefaultEquipmentEnchantments(random,difficulty);
        var result=super.finalizeSpawn(world,difficulty,reason,group,tag); setLeftHanded(false); return result;
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); if (hasRestriction()) { tag.putLong("Home",getRestrictCenter().asLong()); tag.putInt("HomeDistance",(int)getRestrictRadius()); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); if (tag.contains("HomeDistance")) restrictTo(BlockPos.of(tag.getLong("Home")),Math.max(0,tag.getInt("HomeDistance")));
    }
}
