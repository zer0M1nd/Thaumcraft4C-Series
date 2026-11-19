package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Short-lived vis pickups. Collection checks the hotbar in order, then the offhand. */
public  class AspectOrbEntity extends Entity {
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(AspectOrbEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> VALUE = SynchedEntityData.defineId(AspectOrbEntity.class, EntityDataSerializers.INT);
    private int age, cooldown, health = 5;
    private Player target;
    public AspectOrbEntity(EntityType<? extends AspectOrbEntity> type, Level level) { super(type, level); }
    public AspectOrbEntity(Level level, double x, double y, double z, Aspect aspect, int value) {
        this(TCEntities.ASPECT_ORB.get(), level); setPos(x, y, z);
        entityData.set(ASPECT, aspect.tag()); entityData.set(VALUE, value);
        setYRot(random.nextFloat() * 360);
        setDeltaMovement((random.nextDouble() * (double) .2f - (double) .1f) * 2,
                random.nextDouble() * .4, (random.nextDouble() * (double) .2f - (double) .1f) * 2);
    }
    @Override protected void defineSynchedData() { entityData.define(ASPECT, "aer"); entityData.define(VALUE, 1); }
    public Aspect aspect() { return TCAspects.catalog().require(entityData.get(ASPECT)); }
    public int value() { return entityData.get(VALUE); }
    public int age() { return age; }
    public int maxAge() { return 150; }
    private int hotbar(Player player) {
        for (int slot = 0; slot < InventorySize.HOTBAR; slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof CastingWandItem wand && wand.receiveVis(stack, aspect(), value() * 100, true) > 0) return slot;
        }
        var offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof CastingWandItem wand && wand.receiveVis(offhand, aspect(), value() * 100, true) > 0)
            return 40; // Inventory's offhand compartment follows the 36 main and four armor slots.
        return -1;
    }
    private static  class InventorySize { static final int HOTBAR = 9; }
    @Override public void tick() {
        super.tick(); if (cooldown > 0) cooldown--;
        setDeltaMovement(getDeltaMovement().add(0, -(double) .03f, 0));
        if (level().getFluidState(blockPosition()).is(FluidTags.LAVA)) {
            setDeltaMovement((random.nextFloat() - random.nextFloat()) * .2f, .2f, (random.nextFloat() - random.nextFloat()) * .2f);
            playSound(SoundEvents.GENERIC_BURN, .4f, 2 + random.nextFloat() * .4f);
        }
        // Modern Entity applies an escape impulse even in empty space. As with vanilla
        // experience orbs, only ask for that impulse when the pickup is inside a block.
        if (!level().noCollision(getBoundingBox()))
            moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2, getZ());
        if (target != null && (target.isRemoved() || target.level() != level())) target = null;
        if (target == null && tickCount % 5 == 0) {
            double nearest = Double.MAX_VALUE;
            for (var player : level().getEntitiesOfClass(Player.class, new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(8))) {
                double distance = distanceToSqr(player);
                if (distance < nearest && hotbar(player) >= 0) { target = player; nearest = distance; }
            }
        }
        if (target != null) {
            var delta = target.getEyePosition().subtract(position()).scale(1 / 8.0);
            double distance = delta.length(), strength = 1 - distance;
            // Avoid the legacy division by zero when an orb reaches the exact eye position.
            if (strength > 0 && distance > 0) setDeltaMovement(getDeltaMovement().add(delta.scale(strength * strength * .1 / distance)));
        }
        move(MoverType.SELF, getDeltaMovement());
        float drag = .98f;
        if (onGround()) {
            var below = net.minecraft.core.BlockPos.containing(getX(), getBoundingBox().minY - 1, getZ());
            var state = level().getBlockState(below);
            drag = state.isAir() ? .58800006f : state.getFriction(level(), below, this) * .98f;
        }
        setDeltaMovement(getDeltaMovement().multiply(drag, (double) .98f, drag));
        if (onGround()) setDeltaMovement(getDeltaMovement().multiply(1, (double) -.9f, 1));
        if (++age >= maxAge()) discard();
    }
    @Override public void playerTouch(Player player) {
        if (level().isClientSide || isRemoved() || cooldown != 0 || player.takeXpDelay != 0 || !aspect().isPrimal()) return;
        int slot = hotbar(player); if (slot < 0) return;
        var stack = player.getInventory().getItem(slot);
        if (((CastingWandItem) stack.getItem()).receiveVis(stack, aspect(), value() * 100, false) <= 0) return;
        player.takeXpDelay = 2; player.getInventory().setChanged();
        playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, .1f, .5f * ((random.nextFloat() - random.nextFloat()) * .7f + 1.8f)); discard();
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt(); health = (int) (health - amount); if (health <= 0) discard(); return false;
    }
    @Override public boolean isAttackable() { return false; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 1024; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Health", (short) (byte) health); tag.putShort("Age", (short) age);
        tag.putShort("Value", (short) value()); tag.putString("Aspect", aspect().tag());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        health = tag.getShort("Health") & 255; age = tag.getShort("Age");
        String aspect = tag.getString("Aspect"); int value = tag.getShort("Value");
        // Invalid old data could crash collection; discard it rather than inventing charge.
        if (TCAspects.catalog().all().stream().noneMatch(a -> a.tag().equals(aspect)) || value <= 0) { discard(); return; }
        entityData.set(ASPECT, aspect); entityData.set(VALUE, value);
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
