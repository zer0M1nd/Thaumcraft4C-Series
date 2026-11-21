package thaumicfutures.devices;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import tc4revival.content.*;

/** TC5's basic turret: physical arrows, hostile targets, and an upward dispenser beneath it. */
public class AutomatedCrossbow extends PathfinderMob implements Container,MenuProvider,OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(AutomatedCrossbow.class,EntityDataSerializers.OPTIONAL_UUID);
    private int cooldown=20,shotTicks,loadTicks;
    public AutomatedCrossbow(EntityType<? extends AutomatedCrossbow> type,Level level) {
        super(type,level);setPersistenceRequired();xpReward=5;setMaxUpStep(0);
        lookControl=new net.minecraft.world.entity.ai.control.LookControl(this) { @Override public void tick() {} };
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ARMOR,2).add(Attributes.FOLLOW_RANGE,24)
                .add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,.9);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(OWNER,Optional.empty()); }
    @Override public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }
    public void setOwnerUUID(UUID id) { entityData.set(OWNER,Optional.ofNullable(id)); }
    @Override public LivingEntity getOwner() { return getOwnerUUID()==null?null:level().getPlayerByUUID(getOwnerUUID()); }
    public boolean canManage(Player player) { return isAlive() && (player.getUUID().equals(getOwnerUUID()) || getOwnerUUID()==null && player.hasPermissions(2)); }
    public static boolean accepts(ItemStack stack) { return stack.getItem() instanceof ArrowItem; }
    @Override public boolean isAlliedTo(Entity other) {
        if(other==this || other.getUUID().equals(getOwnerUUID()))return true;
        if(other instanceof OwnableEntity owned && getOwnerUUID()!=null && getOwnerUUID().equals(owned.getOwnerUUID()))return true;
        var owner=getOwner();return owner!=null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }
    @Override public net.minecraft.world.scores.Team getTeam() { var owner=getOwner();return owner==null?super.getTeam():owner.getTeam(); }
    @Override public boolean canAttack(LivingEntity target) {
        return target!=null && target.isAlive() && !target.isSpectator() && !target.isInvulnerable() && !isAlliedTo(target)
                && (!(target instanceof Player player) || !player.getAbilities().instabuild && getServer()!=null && getServer().isPvpAllowed());
    }
    private boolean validTarget(LivingEntity target) { return canAttack(target) && distanceToSqr(target)<=24*24 && FocusTargeting.visible(this,target.getEyePosition()); }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) { if(shotTicks>0)shotTicks--;if(loadTicks>0)loadTicks--;return; }
        if(!isAlive())return;
        if(tickCount%80==0)heal(1);
        if(getMainHandItem().isEmpty())reload();
        if(!validTarget(getTarget()))setTarget(null);
        if(getTarget()==null && tickCount%5==0) {
            var revenge=getLastHurtByMob();
            setTarget(validTarget(revenge)?revenge:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(24),e->e instanceof Enemy && validTarget(e))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
        }
        if(getTarget()!=null) {
            var aim=getTarget().getEyePosition().subtract(getEyePosition());
            float yaw=(float)(Mth.atan2(aim.z,aim.x)*Mth.RAD_TO_DEG)-90;
            float pitch=(float)(-Mth.atan2(aim.y,Math.sqrt(aim.x*aim.x+aim.z*aim.z))*Mth.RAD_TO_DEG);
            setYHeadRot(Mth.approachDegrees(yHeadRot,yaw,10));setYRot(yHeadRot);setXRot(Mth.approachDegrees(getXRot(),pitch,10));yBodyRot=getYRot();
            if(--cooldown<=0 && accepts(getMainHandItem()) && Math.abs(Mth.wrapDegrees(yaw-yHeadRot))<10 && Math.abs(pitch-getXRot())<10) {
                shoot(getTarget());cooldown=20+Mth.floor(40*Math.min(1,distanceTo(getTarget())/24));
            }
        } else cooldown=20;
    }
    private void reload() {
        var below=blockPosition().below();if(!level().hasChunkAt(below))return;
        var state=level().getBlockState(below);
        if(!(state.getBlock() instanceof DispenserBlock) || state.getValue(DispenserBlock.FACING)!=Direction.UP
                || !(level().getBlockEntity(below) instanceof DispenserBlockEntity source))return;
        for(int slot=0;slot<source.getContainerSize();slot++)if(accepts(source.getItem(slot))) {
            setItem(0,source.removeItem(slot,Math.min(64,source.getItem(slot).getCount())));source.setChanged();
            level().broadcastEntityEvent(this,(byte)61);playSound(SoundEvents.CROSSBOW_LOADING_END,1,1);return;
        }
    }
    private void shoot(LivingEntity target) {
        var ammo=getMainHandItem();if(!(ammo.getItem() instanceof ArrowItem item))return;
        var arrow=item.createArrow(level(),ammo.copyWithCount(1),this);
        float range=Mth.clamp(distanceTo(target)/24,.1f,1);
        var origin=getEyePosition().add(getLookAngle().scale(.35));arrow.setPos(origin);
        var aim=target.getEyePosition().subtract(origin).add(0,range*range*4,0);
        arrow.setBaseDamage((item instanceof PrimalArrowItem?3:2.5)+range*2+random.nextGaussian()*.25);
        arrow.shoot(aim.x,aim.y,aim.z,1.6f,item instanceof PrimalArrowItem?1:2);
        arrow.pickup=AbstractArrow.Pickup.ALLOWED;
        if(getOwnerUUID()!=null)arrow.getPersistentData().putUUID(CrossbowSafety.OWNER,getOwnerUUID());
        if(!level().addFreshEntity(arrow))return;
        ammo.shrink(1);if(ammo.isEmpty())setItem(0,ItemStack.EMPTY);
        playSound(SoundEvents.ARROW_SHOOT,1,1/(random.nextFloat()*.4f+.8f));level().broadcastEntityEvent(this,(byte)60);
    }
    @Override public void handleEntityEvent(byte id) { if(id==60)shotTicks=6;else if(id==61)loadTicks=10;else super.handleEntityEvent(id); }
    public float shotProgress(float partial) { return shotTicks==0?0:Mth.clamp((6-shotTicks+partial)/6,0,1); }
    public float loadProgress(float partial) { return loadTicks==0?0:Mth.clamp((10-loadTicks+partial)/10,0,1); }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) { return size.height*.66f; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void move(MoverType type,Vec3 movement) { super.move(type,new Vec3(movement.x/5,movement.y,movement.z/5)); }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(!canManage(player))return InteractionResult.PASS;
        if(!level().isClientSide) {
            if(player.getItemInHand(hand).getItem() instanceof CastingWandItem) {
                dropAmmo();var item=new ItemStack(Devices.CROSSBOW_ITEM.get());if(hasCustomName())item.setHoverName(getCustomName());
                spawnAtLocation(item,.5f);discard();playSound(TCSounds.ZAP.get(),1,1);
            } else if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,this,buf->buf.writeInt(getId()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private void dropAmmo() { var ammo=removeItemNoUpdate(0);if(!ammo.isEmpty())spawnAtLocation(ammo); }
    @Override protected void dropEquipment() { dropAmmo(); }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(random.nextFloat()<.2f+looting*.15f)spawnAtLocation(TCItems.ZOMBIE_BRAIN.get());
        if(random.nextFloat()<.5f+looting*.15f)spawnAtLocation(thaumicfutures.LegacyContent.BRASS.get());
        var planks=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:greatwood_planks"));
        for(int i=0;i<2;i++)if(planks!=null && random.nextFloat()<.5f+looting*.15f)spawnAtLocation(planks);
    }
    @Override public ItemStack getPickResult() { return new ItemStack(Devices.CROSSBOW_ITEM.get()); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new CrossbowMenu(id,inventory,this); }
    @Override public Component getDisplayName() { return super.getDisplayName(); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return getMainHandItem().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot==0?getMainHandItem():ItemStack.EMPTY; }
    @Override public void setItem(int slot,ItemStack stack) { if(slot==0)setItemSlot(EquipmentSlot.MAINHAND,stack); }
    @Override public ItemStack removeItem(int slot,int amount) { return slot==0 && amount>0?getMainHandItem().split(amount):ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var item=getItem(slot);if(slot==0)setItem(0,ItemStack.EMPTY);return item; }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return canManage(player) && player.level()==level() && player.distanceToSqr(this)<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0 && accepts(stack); }
    @Override public void clearContent() { setItem(0,ItemStack.EMPTY); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);if(getOwnerUUID()!=null)tag.putUUID("Owner",getOwnerUUID());tag.putInt("FireCooldown",Math.max(0,cooldown)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);setOwnerUUID(tag.hasUUID("Owner")?tag.getUUID("Owner"):null);cooldown=Mth.clamp(tag.getInt("FireCooldown"),0,60); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
