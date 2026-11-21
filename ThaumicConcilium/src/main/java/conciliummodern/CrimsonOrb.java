package conciliummodern;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;

/** The original colliding boomerang, with saved ownership and server-only ward removal. */
public final class CrimsonOrb extends Entity {
    private UUID ownerId;private int age,lifetime=100,power=2;
    public CrimsonOrb(EntityType<? extends CrimsonOrb> type,Level level){super(type,level);}
    public CrimsonOrb(LivingEntity owner){this(CrimsonContent.ORB.get(),owner.level());ownerId=owner.getUUID();setPos(owner.getEyePosition().add(owner.getLookAngle()));setDeltaMovement(owner.getLookAngle());}
    @Override protected void defineSynchedData(){}
    public LivingEntity owner(){return ownerId!=null&&level() instanceof ServerLevel server&&server.getEntity(ownerId) instanceof LivingEntity entity?entity:null;}
    public int age(){return age;}
    @Override public void tick(){
        super.tick();var next=net.minecraft.core.BlockPos.containing(position().add(getDeltaMovement()));
        if(!level().hasChunkAt(next)||!level().getWorldBorder().isWithinBounds(next)){if(!level().isClientSide)discard();return;}
        move(MoverType.SELF,getDeltaMovement());age++;
        if(level() instanceof ServerLevel level){
            var owner=owner();if(owner==null){discard();return;}
            for(var target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox(),e->e.isAlive()&&!e.isSpectator()&&e!=owner&&!(e instanceof CultistMob)&&!e.isAlliedTo(owner))){
                if(target instanceof net.minecraft.world.entity.player.Player p&&p.isCreative())continue;
                if(!target.hurt(damageSources().indirectMagic(this,owner),10+power))continue;
                target.setAbsorptionAmount(0);
                if(target instanceof ServerPlayer player){
                    var shield=RunicShieldEvents.state(player);var equipment=shield.equipment();
                    shield.equipment(new RunicShieldState.Equipment(0,equipment.charged(),equipment.kinetic(),equipment.healing(),equipment.emergency()));shield.equipment(equipment);
                    tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),new tc4revival.network.RunicChargePacket(player.getId(),0,Math.max(0,shield.maximum())));
                }
            }
            if(age==50){setDeltaMovement(getDeltaMovement().scale(-1));hasImpulse=true;}
            if(age>lifetime)discard();
        }
    }
    @Override public boolean isAttackable(){return false;}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(ownerId!=null)tag.putUUID("Owner",ownerId);tag.putInt("Life",age);tag.putInt("LifeTime",lifetime);tag.putInt("power",power);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){ownerId=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;age=Math.max(0,tag.getInt("Life"));lifetime=net.minecraft.util.Mth.clamp(tag.getInt("LifeTime"),1,1200);power=net.minecraft.util.Mth.clamp(tag.getInt("power"),0,1000);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
