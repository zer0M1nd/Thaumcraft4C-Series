package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Hopping companion; inventory/menu/placer share its persistent owner and upgrade state. */
public  class TravelingTrunkEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(TravelingTrunkEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> UPGRADE=SynchedEntityData.defineId(TravelingTrunkEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(TravelingTrunkEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STAY=SynchedEntityData.defineId(TravelingTrunkEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> OPEN=SynchedEntityData.defineId(TravelingTrunkEntity.class,EntityDataSerializers.BOOLEAN);
    // Old InventoryTrunk always allocates 36 slots; the earth upgrade exposes the last row.
    public final SimpleContainer inventory=new SimpleContainer(36);
    private int jumpDelay;
    private float lid;
    private float airSpeed=.02f;
    public TravelingTrunkEntity(EntityType<? extends TravelingTrunkEntity> type,Level level) {
        super(type,level); jumpDelay=random.nextInt(20)+10; setPersistenceRequired(); setHealth(getMaxHealth()); setSpeed(.1f); setZza(0);
        ((net.minecraft.world.entity.ai.navigation.GroundPathNavigation)getNavigation()).setCanOpenDoors(true);
    }
    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,75).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this,1.2,true) {
            @Override public boolean canUse(){return !stay() && anger()>0 && super.canUse();}
            @Override public boolean canContinueToUse(){return !stay() && anger()>0 && super.canContinueToUse();}
        });
        goalSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this,true));
        goalSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.Goal() {
            private int recalc;
            {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return !stay() && anger()==0 && owner()!=null && distanceToSqr(owner())>25;}
            @Override public boolean canContinueToUse(){return !stay() && anger()==0 && owner()!=null && distanceToSqr(owner())>9;}
            @Override public void start(){recalc=0;}
            @Override public void stop(){getNavigation().stop();}
            @Override public void tick(){
                var player=owner();if(player==null)return;
                getLookControl().setLookAt(player,30,30);
                if(--recalc<=0){recalc=10;getNavigation().moveTo(player,upgrade()==0?1.5:1.1);}
                if(onGround() && jumpDelay--<=0){jumpDelay=5;getJumpControl().jump();playSound(SoundEvents.CHEST_CLOSE,.1f,random.nextFloat()*.1f+.9f);}
            }
        });
        goalSelector.addGoal(4,new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,Player.class,6));
    }
    @Override public boolean doHurtTarget(Entity target) {
        if(!super.doHurtTarget(target))return false;
        level().broadcastEntityEvent(this,(byte)17);playSound(SoundEvents.BLAZE_HURT,.5f,random.nextFloat()*.1f+.9f);return true;
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(OWNER,""); entityData.define(UPGRADE,-1); entityData.define(ANGER,0); entityData.define(STAY,false); entityData.define(OPEN,false); }
    public String ownerName() { return entityData.get(OWNER); }
    public void ownerName(String name) { entityData.set(OWNER,name); }
    public Player owner() { return level().players().stream().filter(player->player.getGameProfile().getName().equals(ownerName())).findFirst().orElse(null); }
    public int upgrade() { return entityData.get(UPGRADE); }
    public void upgrade(int value) { entityData.set(UPGRADE,value>=-1 && value<6?value:-1); }
    public int rows() { return upgrade()==1?4:3; }
    public int anger() { return entityData.get(ANGER); }
    public void anger(int value) { entityData.set(ANGER,Math.max(0,Math.min(32767,value))); }
    public boolean stay() { return entityData.get(STAY); }
    public void stay(boolean value) { entityData.set(STAY,value); }
    public boolean open() { return entityData.get(OPEN); }
    public void open(boolean value) { entityData.set(OPEN,value); }
    public float lid() { return lid; }
    public boolean mayOpen(Player player) { return upgrade()!=3 || ownerName().equals(player.getGameProfile().getName()); }
    @Override protected net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand) {
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if (held.getItem() instanceof GolemBellItem)
            return mayOpen(player)?net.minecraft.world.InteractionResult.PASS:net.minecraft.world.InteractionResult.SUCCESS;
        if (upgrade()==-1 && held.getItem() instanceof GolemComponentItem component && component.kind()==GolemComponentItem.Kind.UPGRADE) {
            if (!level().isClientSide) {
                upgrade(component.legacyValue()); if(!player.getAbilities().instabuild)held.shrink(1);
                playSound(TCSounds.GOLEM_UPGRADE.get(),.5f,1);
            }
            player.swing(hand);
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        var food=held.getFoodProperties(this);
        // Feeding and installing upgrades precede the water upgrade's owner restriction.
        if (food!=null && getHealth()<getMaxHealth()) {
            if (!level().isClientSide) {
                if(!player.getAbilities().instabuild)held.shrink(1); heal(food.getNutrition());
                playSound(getHealth()==getMaxHealth()?SoundEvents.PLAYER_BURP:SoundEvents.GENERIC_EAT,.5f,random.nextFloat()*.5f+.5f);
                level().broadcastEntityEvent(this,(byte)18);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (mayOpen(player) && player instanceof net.minecraft.server.level.ServerPlayer server)
            net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider(
                    (id,inventory,viewer)->new TravelingTrunkMenu(TCArcane.TRUNK_MENU.get(),id,inventory,this),getDisplayName()),buffer->{ buffer.writeInt(getId()); buffer.writeByte(upgrade()); });
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean fireImmune() { return true; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    @Override protected float getFlyingSpeed() { return airSpeed; }
    @Override public boolean hurt(DamageSource source,float damage) { return !source.is(DamageTypes.CACTUS) && upgrade()!=3 && super.hurt(source,damage); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putString("Owner",ownerName()); tag.putByte("upgrade",(byte)upgrade()); tag.putBoolean("Stay",stay()); tag.put("Inventory",saveInventory());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); ownerName(tag.getString("Owner")); upgrade(tag.getByte("upgrade")); stay(tag.getBoolean("Stay")); loadInventory(tag.getList("Inventory",Tag.TAG_COMPOUND));
    }
    public ListTag saveInventory() {
        var list=new ListTag();
        for (int slot=0;slot<36;slot++) if (!inventory.getItem(slot).isEmpty()) { var tag=inventory.getItem(slot).save(new CompoundTag()); tag.putByte("Slot",(byte)slot); list.add(tag); }
        return list;
    }
    public void loadInventory(ListTag list) {
        inventory.clearContent();
        for (Tag value:list) if (value instanceof CompoundTag tag) { int slot=tag.getByte("Slot")&255; if (slot<36) inventory.setItem(slot,ItemStack.of(tag)); }
    }
    @Override public void aiStep() {
        super.aiStep(); if (upgrade()==5) pullItems();
        if (isInWater()) setDeltaMovement(getDeltaMovement().add(0,(double).033f,0));
        if (level().isClientSide) {
            if (!onGround() && getDeltaMovement().y<0 && !isInWater()) lid+=.015f;
            if ((onGround() || isInWater()) && !open()) lid=Math.max(0,lid-.1f);
            if (open()) lid+=.035f; lid=Math.min(lid,open()?.5f:.2f);
        } else if (getHealth()<getMaxHealth() && (upgrade()==3 || tickCount%50==0)) heal(1);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (anger()>0) anger(anger()-1);fallDistance=0;
        airSpeed=upgrade()==0?.04f:.03f;
        var owner=owner();if(owner==null){setTarget(null);getNavigation().stop();return;}
        TravelingTrunkFollowers.track(this);
        if(stay()){setTarget(null);getNavigation().stop();return;}
        if(distanceTo(owner)>20 && teleportNear(owner)){getNavigation().stop();return;}
        if(getTarget()!=null && (!getTarget().isAlive() || anger()==0))setTarget(null);
        var attacker=owner.getLastHurtByMob();
        if(upgrade()==2 && anger()==0 && getTarget()==null && attacker!=null && attacker!=owner
                && attacker.isAlive() && canAttack(attacker) && !owner.isAlliedTo(attacker) && hasLineOfSight(attacker)) {
            anger(600);setTarget(attacker);
        }
    }

    private boolean teleportNear(Player owner) {
        int baseX=net.minecraft.util.Mth.floor(owner.getX())-2,baseZ=net.minecraft.util.Mth.floor(owner.getZ())-2,y=net.minecraft.util.Mth.floor(owner.getBoundingBox().minY);
        for (int x=0;x<=4;x++) for (int z=0;z<=4;z++) {
            if (x>=1 && z>=1 && x<=3 && z<=3) continue;
            var pos=new BlockPos(baseX+x,y,baseZ+z); if (!level().hasChunkAt(pos) || level().isOutsideBuildHeight(pos.above())) continue;
            var below=level().getBlockState(pos.below());
            if ((!below.isRedstoneConductor(level(),pos.below()) && !below.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) || level().getBlockState(pos).isRedstoneConductor(level(),pos) || level().getBlockState(pos.above()).isRedstoneConductor(level(),pos.above())) continue;
            level().playSound(null,pos,SoundEvents.ENDERMAN_TELEPORT,SoundSource.NEUTRAL,.5f,1); moveTo(pos.getX()+.5,y,pos.getZ()+.5,getYRot(),getXRot()); setTarget(null); return true;
        }
        return false;
    }
    @Override public Entity changeDimension(net.minecraft.server.level.ServerLevel destination,net.minecraftforge.common.util.ITeleporter teleporter) {
        if (stay() || !isAlive() || level()==destination) return null;
        if (owner()!=null) return super.changeDimension(destination,teleporter);
        var owner=destination.players().stream().filter(player->player.getGameProfile().getName().equals(ownerName())).findFirst().orElse(null);
        if (owner==null) return null;
        var info=new net.minecraft.world.level.portal.PortalInfo(owner.position().add(0,.25,0),getDeltaMovement(),getYRot(),getXRot());
        return super.changeDimension(destination,new net.minecraftforge.common.util.ITeleporter() {
            @Override public net.minecraft.world.level.portal.PortalInfo getPortalInfo(Entity entity,net.minecraft.server.level.ServerLevel target,java.util.function.Function<net.minecraft.server.level.ServerLevel,net.minecraft.world.level.portal.PortalInfo> vanilla) { return info; }
            @Override public Entity placeEntity(Entity entity,net.minecraft.server.level.ServerLevel source,net.minecraft.server.level.ServerLevel target,float yaw,java.util.function.Function<Boolean,Entity> reposition) { return reposition.apply(false); }
        });
    }
    private void pullItems() {
        if (!isAlive()) return;
        if (!level().isClientSide) for (var item:level().getEntitiesOfClass(ItemEntity.class,new AABB(position().add(-.5,-.5,-.5),position().add(.5,.5,.5)))) {
            var original=item.getItem();
            var slots=new net.minecraftforge.items.wrapper.InvWrapper(inventory) { @Override public int getSlots() { return 27; } };
            var remainder=GolemItemTransfer.insert(slots,original,true);
            if (remainder.getCount()==original.getCount()) continue;
            playSound(SoundEvents.GENERIC_EAT,.5f,random.nextFloat()*.5f+.5f); level().broadcastEntityEvent(this,(byte)17);
            if (remainder.isEmpty()) item.discard(); else item.setItem(remainder);
        }
        for (var item:level().getEntitiesOfClass(ItemEntity.class,new AABB(position().add(-3,-3,-3),position().add(3,3,3)))) {
            var offset=item.position().subtract(position()).add(0,getBbHeight()*.8f,0); double length=(float)Math.sqrt(offset.lengthSqr());
            if (length>1e-9) item.setDeltaMovement(item.getDeltaMovement().subtract(offset.scale(.075/length)));
        }
    }
    public void dropInventory() { for (int slot=0;slot<36;slot++) { var stack=inventory.getItem(slot); if (!stack.isEmpty()) spawnAtLocation(stack); inventory.setItem(slot,ItemStack.EMPTY); } }
    @Override public void die(DamageSource source) { if (!level().isClientSide) dropInventory(); super.die(source); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOOD_STEP; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ITEM_BREAK; }
    @Override protected float getSoundVolume() { return .5f; }
    @Override public void handleEntityEvent(byte event) {
        if (event==17 || event==18) {
            lid=.15f;
            if (event==18) level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART,getX()+random.nextFloat()*getBbWidth()*2-getBbWidth(),getY()+.5+random.nextFloat()*getBbHeight(),getZ()+random.nextFloat()*getBbWidth()*2-getBbWidth(),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);
        } else super.handleEntityEvent(event);
    }
}
