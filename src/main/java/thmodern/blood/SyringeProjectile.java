package thmodern.blood;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thmodern.Horizons;

/** Native arrow collision, enchantments, embedding and pickup, with a full-strength injection payload. */
public final class SyringeProjectile extends AbstractArrow implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> PAYLOAD=SynchedEntityData.defineId(SyringeProjectile.class,EntityDataSerializers.ITEM_STACK);
    public SyringeProjectile(EntityType<? extends SyringeProjectile> type,Level level){super(type,level);setBaseDamage(1.5);}
    public SyringeProjectile(Level level,LivingEntity owner,ItemStack payload){this(Horizons.SYRINGE_PROJECTILE.get(),level);setOwner(owner);setPos(owner.getX(),owner.getEyeY()-.1,owner.getZ());entityData.set(PAYLOAD,payload.copyWithCount(1));pickup=Pickup.ALLOWED;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PAYLOAD,ItemStack.EMPTY);}
    @Override protected void doPostHurtEffects(LivingEntity target){super.doPostHurtEffects(target);InjectionItem.apply(getItem(),target,getOwner());}
    @Override public ItemStack getItem(){return entityData.get(PAYLOAD).copy();}
    @Override protected ItemStack getPickupItem(){return getItem();}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.put("Injection",getItem().save(new CompoundTag()));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);var item=ItemStack.of(tag.getCompound("Injection"));entityData.set(PAYLOAD,item.is(Horizons.INJECTION.get())?item.copyWithCount(1):ItemStack.EMPTY);if(!item.is(Horizons.INJECTION.get()))discard();}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
