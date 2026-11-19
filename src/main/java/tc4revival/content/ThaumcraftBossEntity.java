package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared old boss damage cap, retaliation, scaling and rewards. */
public abstract class ThaumcraftBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(ThaumcraftBossEntity.class,EntityDataSerializers.INT);
    private static final UUID[] HEALTH_IDS=ids("54d621c1-dd4d-4b43-8bd2-5531c8875797","f51257dc-b7fa-4f7a-92d7-75d68e8592c4","3d6b2e42-4141-4364-b76d-0e8664bbd0bb","02c97a08-801c-4131-afa2-1427a6151934","0f354f6a-33c5-40be-93be-81b1338567f1");
    private static final UUID[] DAMAGE_IDS=ids("534f8c57-929a-48cf-bbd6-0fd851030748","d317a76e-0e7c-4c61-acfd-9fa286053b32","ff462d63-26a2-4363-830e-143ed97e2a4f","cf1eb39e-0c67-495f-887c-0d3080828d2f","3cfab9da-2701-43d8-ac07-885f16fa4117");
    private final Map<Integer,Integer> aggro=new HashMap<>();
    private final ServerBossEvent bossBar=new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    protected int spawnTimer;
    protected ThaumcraftBossEntity(EntityType<? extends ThaumcraftBossEntity> type,Level level) { super(type,level); xpReward=50; }
    private static UUID[] ids(String... values) { return Arrays.stream(values).map(UUID::fromString).toArray(UUID[]::new); }
    public static AttributeSupplier.Builder bossAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.KNOCKBACK_RESISTANCE,.95).add(Attributes.FOLLOW_RANGE,40);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(ANGER,0); }
    public int anger() { return entityData.get(ANGER); }
    public int spawnTimer() { return spawnTimer; }
    public void generateChampionName(ChampionMobs.Kind kind) { setCustomName(Component.translatable(getType().getDescriptionId(),ChampionMobs.name(kind))); }
    @Override public boolean isEffectiveAi() { return spawnTimer<=0 && super.isEffectiveAi(); }
    @Override public boolean isInvulnerableTo(DamageSource source) { return spawnTimer>0 || super.isInvulnerableTo(source); }
    @Override public boolean isPushable() { return spawnTimer<=0 && super.isPushable(); }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public void makeStuckInBlock(BlockState state,Vec3 multiplier) {
        if (!state.is(net.minecraft.world.level.block.Blocks.COBWEB)) super.makeStuckInBlock(state,multiplier);
    }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof EldritchMob; }
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,DifficultyInstance difficulty) {}
    @Override protected void populateDefaultEquipmentEnchantments(net.minecraft.util.RandomSource random,DifficultyInstance difficulty) {}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        // Old bosses do not use vanilla monster equipment/leader randomization here.
        restrictTo(blockPosition(),24); return group;
    }
    @Override public void tick() {
        super.tick(); if (spawnTimer>0) spawnTimer--;
        if (anger()>0) entityData.set(ANGER,anger()-1);
        if (level().isClientSide) {
            if (random.nextInt(15)==0 && anger()>0) level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    getX()+(random.nextFloat()-.5)*getBbWidth(),getY()+getBbHeight()+random.nextFloat()*.5,getZ()+(random.nextFloat()-.5)*getBbWidth(),
                    random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);
            return;
        }
        if (tickCount%30==0) heal(1);
        if (getTarget()!=null && !getTarget().isAlive()) setTarget(null);
        if (getTarget()!=null && tickCount%20==0) updateAggro();
        bossBar.setName(getDisplayName()); bossBar.setProgress(getMaxHealth()>0 ? getHealth()/getMaxHealth() : 0);
    }
    private void updateAggro() {
        int baseline=aggro.getOrDefault(getTarget().getId(),0),largest=baseline,players=0;
        LivingEntity candidate=getTarget();
        var entries=aggro.entrySet().iterator();
        while(entries.hasNext()) {
            var entry=entries.next();var entity=level().getEntity(entry.getKey());
            if(!(entity instanceof LivingEntity living) || !living.isAlive() || !canAttack(living) || distanceToSqr(living)>16384) {entries.remove();continue;}
            if(living instanceof Player)players++;
            int score=entry.getValue();
            if(score>baseline+25L && score>baseline*1.1 && score>largest) {candidate=living;largest=score;}
        }
        if(candidate!=getTarget())setTarget(candidate);
        float previousMax=getMaxHealth(); var health=getAttribute(Attributes.MAX_HEALTH); var attack=getAttribute(Attributes.ATTACK_DAMAGE);
        for (int index=0;index<5;index++) { health.removeModifier(HEALTH_IDS[index]); attack.removeModifier(DAMAGE_IDS[index]); }
        for (int index=0;index<Math.min(5,players-1);index++) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_IDS[index],"TC4 boss health "+index,50,AttributeModifier.Operation.ADDITION));
            attack.addPermanentModifier(new AttributeModifier(DAMAGE_IDS[index],"TC4 boss damage "+index,.5,AttributeModifier.Operation.ADDITION));
        }
        if (previousMax>0) setHealth(getHealth()*getMaxHealth()/previousMax);
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        if (!level().isClientSide) {
            if (source.getEntity() instanceof LivingEntity attacker) aggro.merge(attacker.getId(),(int)Math.min(Integer.MAX_VALUE,Math.max(0,damage)),(a,b)->(int)Math.min(Integer.MAX_VALUE,(long)a+b));
            if (damage>35) {
                if (anger()==0) {
                    addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,(int)(damage/15)));
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(damage/40)));
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,(int)(damage/40)));
                    entityData.set(ANGER,200);
                    if (source.getEntity() instanceof Player player) player.displayClientMessage(getDisplayName().copy().append(" ").append(Component.translatable("tc.boss.enrage")),false);
                }
                damage=35;
            }
        }
        return super.hurt(source,damage);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putInt("Anger",anger());tag.putInt("SpawnTimer",spawnTimer); if (hasRestriction()) { tag.putLong("Home",getRestrictCenter().asLong()); tag.putInt("HomeDistance",(int)getRestrictRadius()); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);entityData.set(ANGER,Math.max(0,tag.getInt("Anger")));spawnTimer=Math.max(0,tag.getInt("SpawnTimer")); if (tag.contains("HomeDistance")) restrictTo(BlockPos.of(tag.getLong("Home")),Math.max(0,tag.getInt("HomeDistance")));
    }
    @Override public net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.item.ItemStack stack,float offset) {
        if(!stack.is(TCItems.PRIMORDIAL_PEARL.get()))return super.spawnAtLocation(stack,offset);
        var item=TCEntities.SPECIAL_ITEM.get().create(level());if(item==null)return null;
        item.setPos(getX(),getY()+getBbHeight()/2,getZ());item.setItem(stack);item.setDefaultPickUpDelay();
        if(captureDrops()!=null)captureDrops().add(item);else level().addFreshEntity(item);return item;
    }

}
