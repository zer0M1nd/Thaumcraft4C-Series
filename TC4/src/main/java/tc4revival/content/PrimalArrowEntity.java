package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

/** Native arrow movement, impact, enchantments, pickup and persistence with TC4 elemental effects. */
public class PrimalArrowEntity extends AbstractArrow implements IEntityAdditionalSpawnData {
    private int element,groundTicks;
    public PrimalArrowEntity(EntityType<? extends PrimalArrowEntity> type,Level level) { super(type,level); }
    public int element() { return element; }
    public int groundTicks() { return groundTicks; }
    public static PrimalArrowEntity create(LivingEntity owner,int element) {
        var arrow=java.util.Objects.requireNonNull(TCEntities.PRIMAL_ARROW.get().create(owner.level()));
        arrow.element=Mth.clamp(element,0,5);arrow.setOwner(owner);
        arrow.setPos(owner.getX(),owner.getEyeY()-.1,owner.getZ());
        arrow.setBaseDamage(2.1*(element==3?1.5:element>=4?.8:1));
        if(element==3)arrow.setKnockback(0);
        arrow.pickup=owner instanceof Player?Pickup.ALLOWED:Pickup.DISALLOWED;
        return arrow;
    }
    public static PrimalArrowEntity launch(LivingEntity owner,float force,int element) {
        var arrow=create(owner,element);
        arrow.shootFromRotation(owner,owner.getXRot(),owner.getYRot(),0,force*1.5f,1);return arrow;
    }
    @Override public void setKnockback(int value) { super.setKnockback(value+(element==3?1:0)); }
    @Override protected ItemStack getPickupItem() { return new ItemStack(TCItems.PRIMAL_ARROWS.get(element).get()); }
    @Override public void tick() {
        super.tick();groundTicks=inGround?groundTicks+1:0;
    }
    public DamageSource elementalDamageSource(DamageSource ordinary) {
        if(element!=0 && element!=1 && element!=4)return ordinary;
        String id=element==0?"air_arrow":element==1?"fire_arrow":"order_arrow";
        var key=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumcraft:"+id));
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),this,getOwner()==null?this:getOwner());
    }
    @Override protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if(element==1)target.setSecondsOnFire(5);
        if(element==2) { target.clearFire(); target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,4),getOwner()); }
        if(element==4)target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,4),getOwner());
        if(element==5)target.addEffect(new MobEffectInstance(MobEffects.WITHER,100),getOwner());
    }
    /** Compatibility entry point for addon callers; normal flight uses AbstractArrow.onHitEntity. */
    public boolean inflictElementDamage(Entity target) {
        int damage=Mth.ceil(Mth.clamp(getDeltaMovement().length()*getBaseDamage(),0,Integer.MAX_VALUE));
        if(isCritArrow())damage=(int)Math.min(Integer.MAX_VALUE,(long)damage+random.nextInt(damage/2+2));
        boolean hit=target.hurt(elementalDamageSource(damageSources().arrow(this,getOwner())),damage);
        if(hit && target instanceof LivingEntity living)doPostHurtEffects(living);
        return hit;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putByte("type",(byte)element);tag.putInt("GroundTicks",groundTicks);tag.putInt("ElementalKnockback",getKnockback());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);element=Mth.clamp(tag.getByte("type"),0,5);groundTicks=Math.max(0,tag.getInt("GroundTicks"));
        super.setKnockback(tag.contains("ElementalKnockback")?Math.max(0,tag.getInt("ElementalKnockback")):element==3?1:0);
        if(!tag.contains("damage"))setBaseDamage(2.1*(element==3?1.5:element>=4?.8:1));
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void writeSpawnData(FriendlyByteBuf data) { data.writeByte(element); }
    @Override public void readSpawnData(FriendlyByteBuf data) { element=Mth.clamp(data.readByte(),0,5); }
}
