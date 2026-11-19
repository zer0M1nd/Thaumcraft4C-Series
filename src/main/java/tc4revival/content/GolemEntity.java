package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import tc4revival.aspect.Aspect;
import tc4revival.config.TCConfig;

/** TC4 work golem. Item/menu entry points configure this entity after assigning its home and owner. */
public  class GolemEntity extends AbstractGolem implements GolemWorkHost, net.minecraft.world.entity.monster.RangedAttackMob {
    private static final EntityDataAccessor<CompoundTag> STATE=SynchedEntityData.defineId(GolemEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private static final GameProfile CHEST_USER=new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(StandardCharsets.UTF_8)),"FakeThaumcraftGolem");
    private final GolemState state=new GolemState(this::publish);
    private final GolemTransportState transport=new GolemTransportState(this);
    private int regeneration,actionTicks,leftTicks,rightTicks,healingTicks;
    private float bootup=-1;
    private boolean retaliating;
    private boolean mobileBindings;
    private java.util.List<net.minecraft.world.level.pathfinder.Path> taskRoute=java.util.List.of();
    private static final EntityDataAccessor<CompoundTag> ROUTE=SynchedEntityData.defineId(GolemEntity.class,EntityDataSerializers.COMPOUND_TAG);
    public void taskRoute(java.util.List<net.minecraft.world.level.pathfinder.Path> paths) { taskRoute=java.util.List.copyOf(paths); syncRoute(); }
    public java.util.List<BlockPos> visibleRoute() { return java.util.Arrays.stream(entityData.get(ROUTE).getLongArray("nodes")).mapToObj(BlockPos::of).toList(); }
    private void syncRoute() {
        if (level().isClientSide) return;
        var nodes=new java.util.ArrayList<BlockPos>();
        var active=getNavigation().getPath();
        if (active!=null && !active.isDone()) for (int i=active.getNextNodeIndex();i<active.getNodeCount() && nodes.size()<1024;i++) nodes.add(active.getNode(i).asBlockPos());
        // The first planned leg is represented by the live navigation, which may have rerouted.
        for (int leg=1;leg<taskRoute.size();leg++) {
            var future=taskRoute.get(leg);
            if (!future.isDone()) for (int i=future.getNextNodeIndex();i<future.getNodeCount() && nodes.size()<1024;i++) nodes.add(future.getNode(i).asBlockPos());
        }
        var tag=new CompoundTag();tag.putLongArray("nodes",nodes.stream().mapToLong(BlockPos::asLong).toArray());
        if (!tag.equals(entityData.get(ROUTE))) entityData.set(ROUTE,tag);
    }
    public boolean mobileBindings(){return mobileBindings;}
    public GolemEntity(EntityType<? extends GolemEntity> type,Level level) {
        super(type,level); setMaxUpStep(1); setPersistenceRequired();
        moveControl=new GolemMoveControl(this);
        if (getNavigation() instanceof GroundPathNavigation navigation) { navigation.setCanOpenDoors(true); navigation.setCanPassDoors(true); navigation.setCanFloat(true); }
        refreshConfiguration();
    }
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) { return new GolemDoorNavigation(this,level); }
    @Override public void move(MoverType type,net.minecraft.world.phys.Vec3 movement) {
        boolean stopX=false,stopZ=false;
        // Speed effects can carry a centered golem past a path node in one tick.
        // Stop at that node rather than oscillating across it or cutting the next corner.
        var path=getNavigation().getPath();
        if(type==MoverType.SELF && state!=null && configuration().axisAligned()
                && !level().isClientSide && onGround() && path!=null && !path.isDone()) {
            var next=path.getNextEntityPos(this);double dx=next.x-getX(),dz=next.z-getZ();
            stopX=movement.x*dx>0 && Math.abs(movement.x)>Math.abs(dx);
            stopZ=movement.z*dz>0 && Math.abs(movement.z)>Math.abs(dz);
            if(stopX||stopZ)movement=new net.minecraft.world.phys.Vec3(stopX?dx:movement.x,movement.y,stopZ?dz:movement.z);
        }
        super.move(type,movement);
        if(stopX||stopZ){var motion=getDeltaMovement();setDeltaMovement(stopX?0:motion.x,motion.y,stopZ?0:motion.z);}
    }
    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,1).add(Attributes.MOVEMENT_SPEED,.6).add(Attributes.FOLLOW_RANGE,32); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(STATE,new CompoundTag()); entityData.define(ROUTE,new CompoundTag()); }
    @Override protected void registerGoals() {}
    private void publish() { if (!level().isClientSide) entityData.set(STATE,state.visualData()); }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key) && level().isClientSide && state!=null) {
            state.applyVisualData(entityData.get(STATE));
            if (bootup<0 && configuration().hasTask()) bootup=0;
        }
    }
    public GolemState state() { return state; }
    @Override public net.minecraft.network.chat.Component getName() { return hasCustomName()?getCustomName():net.minecraft.network.chat.Component.translatable("item.thaumcraft.golem_"+configuration().material().name().toLowerCase(java.util.Locale.ROOT)); }
    public void refreshConfiguration() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(configuration().maxHealth());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(configuration().attackDamage());
        setPathfindingMalus(BlockPathTypes.WATER,configuration().properties().sinks()?0:8);
        if (!level().isClientSide) { filters().configure(configuration()); GolemTaskSet.install(this,goalSelector,targetSelector); publish(); }
    }
    @Override public PathfinderMob mob() { return this; }
    @Override public GolemConfiguration configuration() { return state.configuration(); }
    @Override public GolemFilterInventory filters() { return state.filters(); }
    @Override public GolemMarkers markers() { return state.markers(); }
    @Override public GolemTransportState transport() { return transport; }
    @Override public BlockPos home() { return state.home(); }
    @Override public Direction homeFacing() { return state.homeFacing(); }
    @Override public String ownerName() { return state.owner(); }
    @Override public int workDelay() { return TCConfig.SERVER.golemDelayTicks(); }
    @Override public boolean workEnabled() { return state.workEnabled(); }
    @Override public boolean isNoAi() { return super.isNoAi() || state!=null && !state.workEnabled(); }
    @Override public double navigationSpeed() { return configuration().movementSpeed(state.paused(),state.inactive(),isInWater()); }
    @Override public ItemStack carried() { return state.carried(); }
    @Override public void carried(ItemStack stack) { state.carried(stack); }
    @Override public ItemStack watched() { return state.watched(); }
    @Override public void watched(ItemStack stack) { state.watched(stack); }
    @Override public FluidStack fluid() { return state.fluid(); }
    @Override public void fluid(FluidStack fluid) { state.fluid(fluid); }
    @Override public boolean watchingFluid() { return state.watchingFluid(); }
    @Override public void watchingFluid(boolean value) { state.watchingFluid(value); }
    @Override public Aspect essentia() { return state.essentia(); }
    @Override public int essentiaAmount() { return state.essentiaAmount(); }
    @Override public void essentia(Aspect aspect,int amount) { state.essentia(aspect,amount); }
    @Override public void action() { if (actionTicks==0) { actionTicks=6; level().broadcastEntityEvent(this,(byte)4); } }
    @Override public void leftArm() { if (leftTicks==0) { leftTicks=5; level().broadcastEntityEvent(this,(byte)6); } }
    @Override public void rightArm() { if (rightTicks==0) { rightTicks=5; level().broadcastEntityEvent(this,(byte)8); } }
    public int actionTicks() { return actionTicks; }
    public int leftArmTicks() { return leftTicks; }
    public int rightArmTicks() { return rightTicks; }
    public int healingTicks() { return healingTicks; }
    public float bootup() { return bootup; }
    @Override public void openInventory(BlockPos pos) { inventoryInteraction(pos,true); }
    @Override public void closeInventory(BlockPos pos) { inventoryInteraction(pos,false); }
    private void inventoryInteraction(BlockPos pos,boolean open) {
        if (!(level() instanceof ServerLevel server) || !TCConfig.SERVER.golemChestInteract.get() || !server.hasChunkAt(pos)) return;
        if (server.getBlockEntity(pos) instanceof Container container) {
            var player=FakePlayerFactory.get(server,CHEST_USER);
            if (open) container.startOpen(player); else container.stopOpen(player);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Mob serializes isNoAi(), whose override also includes temporary menu/fetter pauses.
        // Persist only the underlying vanilla flag so an unloaded paused worker can resume.
        tag.putBoolean("NoAI",super.isNoAi());
        state.save(tag);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); state.load(tag); refreshConfiguration();
        // LivingEntity reads Health before the material configuration; restore against the final maximum.
        if (tag.contains("Health",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) setHealth(tag.getFloat("Health"));
        restrictTo(home(),32);
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void setSecondsOnFire(int seconds) { if (state==null || !configuration().properties().fireResistant()) super.setSecondsOnFire(seconds); }
    @Override public LivingEntity getTarget() { var target=super.getTarget(); return target!=null && !target.isAlive()?null:target; }
    @Override public boolean canAttackType(EntityType<?> type) { return type!=EntityType.VILLAGER && type!=TCEntities.GOLEM.get() && type!=EntityType.BAT; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean fireImmune() { return state!=null && configuration().properties().fireResistant() || super.fireImmune(); }
    @Override public int getArmorValue() { return state==null?super.getArmorValue():configuration().armor(super.getArmorValue()); }
    @Override public void aiStep() {
        if(!level().isClientSide){
            boolean moving=GolemMobileBindings.refresh(this);
            if(moving!=mobileBindings){mobileBindings=moving;getNavigation().stop();GolemTaskSet.install(this,goalSelector,targetSelector);}
        }
        super.aiStep();
        if (!level().isClientSide && tickCount%5==0) syncRoute();
        transport.tickCargo();
        var feet=blockPosition(); var below=level().getBlockState(feet.below());
        state.inactive(feet.getY()>level().getMinBuildHeight() && below.is(TCArcane.GOLEM_FETTER.get()) && below.getValue(GolemFetterBlock.POWERED));
        if (actionTicks>0) actionTicks--; if (leftTicks>0) leftTicks--; if (rightTicks>0) rightTicks--; if (healingTicks>0) healingTicks--;
        if (!level().isClientSide) {
            if (regeneration>0) regeneration--;
            else { regeneration=configuration().regenerationDelay(); if (getHealth()<getMaxHealth()) { level().broadcastEntityEvent(this,(byte)5); heal(1); } }
            if (!mobileBindings && (distanceToSqr(home().getX(),home().getY(),home().getZ())>=2304 || isInWall())) returnHome();
        } else if (bootup>0 && configuration().hasTask()) {
            bootup*=bootup/33.1f;
            level().playLocalSound(getX(),getY(),getZ(),TCSounds.CAMERA_TICKS.get(),SoundSource.NEUTRAL,bootup*.2f,bootup,false);
        }
    }
    private void returnHome() {
        for (int y=1;y>=-1;y--) for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            var pos=home().offset(x,y,z); if (!level().hasChunkAt(pos) || level().isOutsideBuildHeight(pos)) continue;
            if (!level().getBlockState(pos.below()).isFaceSturdy(level(),pos.below(),Direction.UP) || level().getBlockState(pos).isSolidRender(level(),pos)) continue;
            if (thaumcraft.api.golems.GolemPathHazards.contains(level().getBlockState(pos)) || thaumcraft.api.golems.GolemPathHazards.contains(level().getBlockState(pos.below()))
                    || !level().getFluidState(pos).isEmpty() || !level().noCollision(this,getBoundingBox().move(pos.getX()+.5-getX(),pos.getY()-getY(),pos.getZ()+.5-getZ()))) continue;
            moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,getYRot(),getXRot()); getNavigation().stop(); return;
        }
    }
    public void recoverStalledTask() {
        if (level().isClientSide) return;
        GolemTaskSet.install(this,goalSelector,targetSelector);
        returnHome();
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        state.paused(false); if (source.is(DamageTypes.CACTUS)) return false;
        if (configuration().material().sinks() && source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumcraft:golem_contact_damage")))) return false;
        if (configuration().material()==GolemMaterial.THAUMIUM && source.is(DamageTypes.MAGIC)) damage*=.5f;
        var attacker=source.getDirectEntity(); int entropy=configuration().upgrades(5);
        if (attacker!=null && attacker!=this && entropy>0 && !retaliating) {
            retaliating=true;
            try { attacker.hurt(damageSources().thorns(this),entropy*2+random.nextInt(entropy*2)); attacker.playSound(SoundEvents.THORNS_HIT,.5f,1); }
            finally { retaliating=false; }
        }
        return super.hurt(source,damage);
    }
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        if (state!=null && configuration().material()==GolemMaterial.THAUMIUM && effect.getEffect().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL) return false;
        return super.canBeAffected(effect);
    }
    @Override protected void actuallyHurt(DamageSource source,float damage) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && configuration().properties().fireResistant()) return;
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) moveTo(home().getX()+.5,home().getY()+.5,home().getZ()+.5,0,0);
        super.actuallyHurt(source,damage);
    }
    @Override public boolean doHurtTarget(Entity target) {
        float damage=(float)getAttributeValue(Attributes.ATTACK_DAMAGE); int knockback=0;
        if (target instanceof LivingEntity living) { damage+=EnchantmentHelper.getDamageBonus(getMainHandItem(),living.getMobType()); knockback=EnchantmentHelper.getKnockbackBonus(this); }
        if (!target.hurt(damageSources().mobAttack(this),damage)) return false;
        if (configuration().decorated('V') && target instanceof LivingEntity living)
            ObfuscationReflectionHelper.setPrivateValue(LivingEntity.class,living,100,"f_20889_");
        if (knockback>0) { target.push(-Math.sin(getYRot()*Math.PI/180)*knockback*.5,.1,Math.cos(getYRot()*Math.PI/180)*knockback*.5); setDeltaMovement(getDeltaMovement().multiply(.6,1,.6)); }
        int fire=EnchantmentHelper.getFireAspect(this)+configuration().upgrades(2); if (fire>0) target.setSecondsOnFire(fire*4);
        if (target instanceof LivingEntity living) EnchantmentHelper.doPostHurtEffects(living,this);
        EnchantmentHelper.doPostDamageEffects(this,target); return true;
    }
    @Override public void dart(LivingEntity target) { GolemDartEntity.fire(this,target); leftArm(); }
    @Override public void performRangedAttack(LivingEntity target,float power) { if (GolemCombatTargets.valid(this,target)) dart(target); }
    @Override protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
        var held=player.getItemInHand(hand);
        // Player.interactOn passes a disposable stack copy to the item callback
        // in creative mode. Bind the actual held bell before that fallback.
        if (held.getItem() instanceof GolemBellItem bell) return bell.interactLivingEntity(held,player,this,hand);
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        if (held.getItem() instanceof GolemBellItem || held.getItem() instanceof GolemComponentItem) return net.minecraft.world.InteractionResult.PASS;
        if (held.is(net.minecraft.world.item.Items.COOKIE)) {
            if (level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.swing(hand);
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    getX(), getY() + getBbHeight() * .6, getZ(), 3, getBbWidth() * .5, getBbHeight() * .3, getBbWidth() * .5, .02);
            playSound(SoundEvents.GENERIC_EAT, .3f, (random.nextFloat() - random.nextFloat()) * .2f + 1);
            var speed=getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
            int duration=Math.min(2400,600+(speed==null?0:speed.getDuration()));
            addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,duration,0));
            heal(5); return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!(held.getItem() instanceof CastingWandItem)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server)
                net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider(
                        (id,inventory,viewer)->new GolemMenu(TCArcane.GOLEM_MENU.get(),id,inventory,this),getDisplayName()),buffer->{ buffer.writeInt(getId()); buffer.writeNbt(state.visualData()); });
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.CAMERA_CLACK.get(); }
    @Override protected float getSoundVolume() { return .1f; }
    public void dropCargo() { if (!level().isClientSide && !carried().isEmpty()) { spawnAtLocation(carried(),.5f); carried(ItemStack.EMPTY); } }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { super.dropCustomDeathLoot(source,looting,recentlyHit); dropCargo(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return TCSounds.CAMERA_CLACK.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.CAMERA_CLACK.get(); }
    @Override public void handleEntityEvent(byte event) {
        switch (event) { case 4->actionTicks=6; case 5->healingTicks=5; case 6->leftTicks=5; case 7->bootup=33; case 8->rightTicks=5; default->super.handleEntityEvent(event); }
    }
}
