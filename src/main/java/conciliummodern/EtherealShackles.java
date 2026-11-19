package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class EtherealShackles extends ThrowableProjectile {
    private int age;
    public EtherealShackles(EntityType<? extends EtherealShackles> type,Level level){super(type,level);}
    public EtherealShackles(LivingEntity owner){this(CrimsonContent.SHACKLES.get(),owner.level());setOwner(owner);setPos(owner.getX(),owner.getEyeY()-.1,owner.getZ());}
    @Override protected void defineSynchedData(){}
    @Override protected float getGravity(){return 0;}
    @Override public void tick(){super.tick();if(++age>100&&!level().isClientSide)discard();}
    private boolean allowed(Entity target){
        if(getOwner() instanceof tc4revival.content.CultistMob&&target instanceof tc4revival.content.CultistMob)return false;
        if(target instanceof Player player){
            if(player.isCreative()||player.isSpectator())return false;
            if(getOwner() instanceof Player caster&&(!caster.canHarmPlayer(player)
                    ||level().getServer()!=null&&!level().getServer().isPvpAllowed()))return false;
        }
        if(getOwner() instanceof LivingEntity owner&&owner.isAlliedTo(target))return false;
        return true;
    }
    @Override protected boolean canHitEntity(Entity target){return super.canHitEntity(target)&&allowed(target);}
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);if(level().isClientSide)return;
        if(hit instanceof EntityHitResult entity&&allowed(entity.getEntity())){
            if(getOwner() instanceof Player player&&net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(player,entity.getEntity()))){discard();return;}
            if(entity.getEntity() instanceof Player player){CrimsonControl.chain(player,100);player.playSound(ConciliumSounds.SHACKLES.get(),.9f,.9f);}
            else if(entity.getEntity() instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,4),getOwner());
        }
        discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Age",age);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);age=Math.max(0,tag.getInt("Age"));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
