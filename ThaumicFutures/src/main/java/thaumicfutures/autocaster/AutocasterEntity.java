package thaumicfutures.autocaster;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import tc4revival.content.*;

/** The equipment slots are the inventory: no shadow wand or duplicated vis reservoir. */
public class AutocasterEntity extends PathfinderMob implements Container,MenuProvider,OwnableEntity {
    private static final EntityDataAccessor<Byte> FACE=SynchedEntityData.defineId(AutocasterEntity.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TARGETS=SynchedEntityData.defineId(AutocasterEntity.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(AutocasterEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> ENABLED=SynchedEntityData.defineId(AutocasterEntity.class,EntityDataSerializers.BOOLEAN);
    private AutocasterCombat combat;
    public AutocasterEntity(EntityType<? extends AutocasterEntity> type,Level level) {
        super(type,level);setPersistenceRequired();setNoGravity(true);xpReward=6;
        // This stationary construct owns both head axes. The default mob controller resets
        // pitch every tick, preventing alignment with close, high or low targets.
        lookControl=new net.minecraft.world.entity.ai.control.LookControl(this) {
            @Override public void tick() {}
        };
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ARMOR,4)
                .add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1);
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();entityData.define(FACE,(byte)Direction.UP.ordinal());entityData.define(TARGETS,(byte)2);
        entityData.define(OWNER,Optional.empty());entityData.define(ENABLED,true);
    }
    public Direction facing() { return Direction.from3DDataValue(entityData.get(FACE)); }
    public void setFacing(Direction face) { entityData.set(FACE,(byte)face.ordinal()); }
    @Override public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }
    public void setOwnerUUID(UUID owner) { entityData.set(OWNER,Optional.ofNullable(owner)); }
    @Override public LivingEntity getOwner() { return getOwnerUUID()==null?null:level().getPlayerByUUID(getOwnerUUID()); }
    public boolean canManage(Player player) { return isAlive() && (player.getUUID().equals(getOwnerUUID()) || getOwnerUUID()==null && player.hasPermissions(2)); }
    public int targetFlags() { return entityData.get(TARGETS)&15; }
    public boolean targets(int bit) { return (targetFlags()&(1<<bit))!=0; }
    public void setTargetFlags(int flags) { entityData.set(TARGETS,(byte)(flags&15));resetCasting(); }
    public boolean enabled() { return entityData.get(ENABLED); }
    public void setEnabled(boolean enabled) { entityData.set(ENABLED,enabled);resetCasting(); }
    public ItemStack focus() { return WandFocusItem.attached(getItem(0)); }
    public double attackRange() { return AutocasterFoci.range(focus()); }
    public boolean protectedAlly(Entity other) {
        if(other==this || other.getUUID().equals(getOwnerUUID()))return true;
        // TC5 treats other autocasters as friendly constructs regardless of their owner.
        if(other instanceof AutocasterEntity && !targets(3))return true;
        if(other instanceof OwnableEntity owned && Objects.equals(owned.getOwnerUUID(),getOwnerUUID()) && getOwnerUUID()!=null)return !targets(3);
        if(AutocasterEffects.belongsTo(other,this))return true;
        var owner=getOwner();return !targets(3) && owner!=null && owner.isAlliedTo(other);
    }
    @Override public boolean isAlliedTo(Entity other) { return protectedAlly(other); }
    @Override public boolean canAttack(LivingEntity other) {
        if(other==null || !other.isAlive() || other.isSpectator() || protectedAlly(other) || other.isInvulnerable())return false;
        if(other instanceof Player player)return targets(2) && !player.getAbilities().instabuild
                && getServer()!=null && getServer().isPvpAllowed();
        if(other instanceof Enemy)return targets(1);
        return other instanceof Mob && targets(0);
    }
    public boolean validTarget(LivingEntity target) {
        double range=attackRange();return target!=null && canAttack(target) && AutocasterFoci.suitable(focus(),target)
                && distanceToSqr(target)<=range*range && FocusTargeting.visible(this,target.getEyePosition());
    }
    @Override public void tick() {
        super.tick();setDeltaMovement(Vec3.ZERO);setNoGravity(true);
        if(level().isClientSide || !isAlive())return;
        if(tickCount%40==0)heal(1);
        // Use the same accepted-centivis transfer as the workbench charger. No aura fabrication,
        // chunk tickets or independent cache; source availability and recharge policies still apply.
        if(AutocasterFoci.acceptsWand(getItem(0)))VisCharging.charge(getItem(0),(aspect,amount)->VisNetwork.drain(level(),blockPosition(),aspect,amount));
        if(!enabled() || !AutocasterFoci.acceptsWand(getItem(0)) || !AutocasterFoci.offensive(focus())) { resetCasting();return; }
        if(!validTarget(getTarget())) { resetCasting(); }
        if(getTarget()==null && tickCount%5==0) {
            var retaliation=getLastHurtByMob();
            var target=validTarget(retaliation)?retaliation:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(attackRange()),this::validTarget)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(target);
        }
        if(getTarget()!=null) {
            if(combat==null)combat=new AutocasterCombat(this);
            combat.tick(getTarget());
        } else if(tickCount%80==0) { setYRot(getYRot()+random.nextFloat()*60-30);setYHeadRot(getYRot()); }
    }
    public void resetCasting() { if(combat!=null)combat.stop();setTarget(null); }
    @Override public void remove(RemovalReason reason) { resetCasting();combat=null;super.remove(reason); }
    @Override public void onRemovedFromWorld() { resetCasting();combat=null;super.onRemovedFromWorld(); }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) { return .45f; }
    @Override public void move(MoverType type,Vec3 movement) {}
    @Override public void travel(Vec3 movement) {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(!canManage(player))return InteractionResult.PASS;
        if(!level().isClientSide) {
            if(player.isShiftKeyDown()) {
                resetCasting();dropContents();var item=new ItemStack(Autocasters.ITEM.get());
                if(hasCustomName())item.setHoverName(getCustomName());spawnAtLocation(item,.5f);discard();
                playSound(thaumicfutures.foci.FutureFoci.ZAP.get(),1,1);
            } else if(player instanceof ServerPlayer server) {
                NetworkHooks.openScreen(server,this,buf->buf.writeInt(getId()));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private void dropContents() {
        for(int slot=0;slot<2;slot++) { var stack=removeItemNoUpdate(slot);if(!stack.isEmpty())spawnAtLocation(stack); }
    }
    @Override protected void dropEquipment() { resetCasting();dropContents(); }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) {
        super.dropCustomDeathLoot(source,looting,recentlyHit);
        if(random.nextFloat()<.2f+looting*.15f)spawnAtLocation(TCItems.ZOMBIE_BRAIN.get());
        if(random.nextFloat()<.5f+looting*.15f)spawnAtLocation(thaumicfutures.LegacyContent.BRASS.get());
        for(int i=0;i<2;i++)if(random.nextFloat()<.5f+looting*.15f) {
            var plank=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:greatwood_planks"));
            if(plank!=null)spawnAtLocation(plank);
        }
    }
    @Override public ItemStack getPickResult() { return new ItemStack(Autocasters.ITEM.get()); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new AutocasterMenu(id,inventory,this); }
    @Override public Component getDisplayName() { return super.getDisplayName(); }
    @Override public int getContainerSize() { return 2; }
    @Override public int getMaxStackSize() { return 1; }
    private EquipmentSlot equipment(int slot) { return slot==0?EquipmentSlot.MAINHAND:EquipmentSlot.CHEST; }
    @Override public ItemStack getItem(int slot) { return slot>=0 && slot<2?getItemBySlot(equipment(slot)):ItemStack.EMPTY; }
    @Override public boolean isEmpty() { return getItem(0).isEmpty() && getItem(1).isEmpty(); }
    @Override public void setItem(int slot,ItemStack stack) {
        if(slot<0 || slot>1)return;
        resetCasting();setItemSlot(equipment(slot),stack);setChanged();
    }
    @Override public ItemStack removeItem(int slot,int amount) {
        if(amount<=0 || slot<0 || slot>1)return ItemStack.EMPTY;
        resetCasting();var result=getItem(slot).split(amount);setChanged();return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if(slot<0 || slot>1)return ItemStack.EMPTY;
        resetCasting();var item=getItem(slot);setItemSlot(equipment(slot),ItemStack.EMPTY);return item;
    }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return canManage(player) && player.level()==level() && player.distanceToSqr(this)<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0?AutocasterFoci.acceptsWand(stack):slot==1 && AutocasterFoci.acceptsBattery(stack); }
    @Override public void clearContent() { resetCasting();setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putByte("Facing",(byte)facing().ordinal());tag.putByte("Targets",(byte)targetFlags());tag.putBoolean("Enabled",enabled());
        if(getOwnerUUID()!=null)tag.putUUID("Owner",getOwnerUUID());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);setFacing(Direction.from3DDataValue(tag.getByte("Facing")));setTargetFlags(tag.contains("Targets")?tag.getByte("Targets"):2);
        setEnabled(!tag.contains("Enabled") || tag.getBoolean("Enabled"));setOwnerUUID(tag.hasUUID("Owner")?tag.getUUID("Owner"):null);
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
