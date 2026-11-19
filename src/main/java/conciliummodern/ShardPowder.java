package conciliummodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import tc4revival.content.*;

public class ShardPowder extends Entity implements net.minecraftforge.entity.IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(ShardPowder.class,EntityDataSerializers.INT);
    private int age,health=5,iteration;
    private UUID ownerId;
    private Player attracted;
    public ShardPowder(EntityType<? extends ShardPowder> type,Level level){super(type,level);}
    public ShardPowder(LivingEntity owner,PowderType type,Vec3 position){this(ConciliumEntities.SHARD_POWDER.get(),owner.level());setOwner(owner);powderType(type);setPos(position);setYRot(random.nextFloat()*360);}
    @Override protected void defineSynchedData(){entityData.define(TYPE,0);}
    public PowderType powderType(){return PowderType.byId(entityData.get(TYPE));}
    public void powderType(PowderType type){entityData.set(TYPE,type.ordinal());}
    public int age(){return age;}
    public void initialAge(int value){age=net.minecraft.util.Mth.clamp(value,0,39);}
    public int iteration(){return iteration;}
    public void setOwner(Entity owner){ownerId=owner==null?null:owner.getUUID();}
    public Entity getOwner(){return ownerId!=null && level() instanceof ServerLevel server?server.getEntity(ownerId):null;}
    @Override public void tick(){
        super.tick();if(!level().hasChunkAt(blockPosition())){if(!level().isClientSide)discard();return;}
        setDeltaMovement(getDeltaMovement().add(0,-.03,0));
        if(isInLava()){setDeltaMovement((random.nextFloat()-random.nextFloat())*.2,.2,(random.nextFloat()-random.nextFloat())*.2);playSound(net.minecraft.sounds.SoundEvents.GENERIC_BURN,.4f,2+random.nextFloat()*.4f);}
        if(!level().noCollision(getBoundingBox()))moveTowardsClosestSpace(getX(),(getBoundingBox().minY+getBoundingBox().maxY)/2,getZ());
        if(powderType()==PowderType.TAINT){
            if(attracted!=null && (!attracted.isAlive() || attracted.level()!=level() || attracted.isSpectator()))attracted=null;
            if(attracted==null && tickCount%5==0)attracted=level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(8),p->p!=getOwner() && p.isAlive() && !p.isSpectator()).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if(attracted!=null){var delta=attracted.getEyePosition().subtract(position()).scale(1d/8);double distance=delta.length(),strength=1-distance;if(strength>0 && distance>1e-8)setDeltaMovement(getDeltaMovement().add(delta.scale(strength*strength*.1/distance)));}
        }
        move(MoverType.SELF,getDeltaMovement());
        var below=BlockPos.containing(getX(),getBoundingBox().minY-1,getZ());
        float friction=onGround()?level().getBlockState(below).getFriction(level(),below,this)*.98f:.98f;
        setDeltaMovement(getDeltaMovement().multiply(friction,onGround()?-.882:.98,friction));
        if(++age>=40 && !level().isClientSide){performEffect();discard();}
    }
    private boolean mayChange(BlockPos pos){
        if(!(level() instanceof ServerLevel level) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos))return false;
        var owner=getOwner();
        if(owner instanceof Player player){if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,powderType().shard()))return false;}
        else if(!ForgeEventFactory.getMobGriefingEvent(level,owner==null?this:owner))return false;
        return !ForgeEventFactory.onBlockPlace(owner==null?this:owner,BlockSnapshot.create(level.dimension(),level,pos),Direction.UP);
    }
    private boolean canAffect(LivingEntity target){
        if(!target.isAlive() || target.isSpectator())return false;
        return !(target instanceof Player player && getOwner() instanceof Player owner && owner!=player && (!((ServerLevel)level()).getServer().isPvpAllowed() || !owner.canHarmPlayer(player)));
    }
    public void performEffect(){
        if(!(level() instanceof ServerLevel level))return;
        var nearby=level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(1.5),this::canAffect);
        LivingEntity first=nearby.isEmpty()?null:nearby.get(0);var owner=getOwner();
        switch(powderType()){
            case AIR->{if(random.nextInt(10)>8 && mayChange(blockPosition())){var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt!=null){bolt.setPos(position());if(owner instanceof ServerPlayer player)bolt.setCause(player);level.addFreshEntity(bolt);}}}
            case FIRE->{if(random.nextInt(10)>8)level.explode(owner,getX(),getY(),getZ(),1.5f,mayChange(blockPosition()),Level.ExplosionInteraction.NONE);}
            case WATER->{for(var pos:List.of(blockPosition().below(),blockPosition()))if(mayChange(pos))BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL),level,pos,owner instanceof Player player?player:null);}
            case EARTH->{if(level.getBlockState(blockPosition()).canBeReplaced() && mayChange(blockPosition()))level.setBlockAndUpdate(blockPosition(),Blocks.DIRT.defaultBlockState());}
            case ORDER->{if(first!=null && first.getAttribute(Attributes.ATTACK_DAMAGE)!=null)first.hurt(level.damageSources().indirectMagic(this,owner),(float)first.getAttributeValue(Attributes.ATTACK_DAMAGE));}
            case ENTROPY->{if(first!=null)first.addEffect(new MobEffectInstance(MobEffects.WITHER,90,50));}
            case WRATH->{if(first!=null){first.setSecondsOnFire(50000);first.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,5));first.addEffect(new MobEffectInstance(TCEffects.INFECTIOUS_VIS_EXHAUST.get(),200,10));}}
            case ENVY->{for(var projectile:level.getEntitiesOfClass(Projectile.class,getBoundingBox().inflate(10),Entity::isAlive)){
                if(owner instanceof LivingEntity living && iteration<5){var child=new ShardPowder(living,PowderType.ENVY,projectile.position());child.iteration=iteration+1;if(level.addFreshEntity(child))projectile.discard();}
                else projectile.discard();
            }}
            case TAINT->{if(first instanceof ServerPlayer player)ConciliumFocus.changeWarp(player,tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY,20);else if(first!=null)first.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),80,50));}
            // Intentional adaptation: original passed an absolute coordinate made from relative X/Z,
            // which could send victims towards the world origin. Keep the relative horizontal shove.
            case PRIDE->{if(first!=null){var delta=first.position().subtract(position());first.setDeltaMovement(new Vec3(delta.x,0,delta.z).normalize().scale(3));first.hurtMarked=true;}}
            case LUST->{if(random.nextInt(10)>8){var tentacle=TCEntities.TAINTACLE_SMALL.get().create(level);if(tentacle!=null){tentacle.setPos(position());if(level.addFreshEntity(tentacle))level.getEntitiesOfClass(LivingEntity.class,tentacle.getBoundingBox().inflate(20.5,2.5,20.5),e->!(e instanceof TaintedMob) && canAffect(e)).stream().findFirst().ifPresent(tentacle::setTarget);}}}
            case SLOTH->{if(first!=null){first.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,15));first.addEffect(new MobEffectInstance(TCEffects.INFECTIOUS_VIS_EXHAUST.get(),60,20));}}
            case GREED->{if(first!=null){var delta=position().subtract(first.position().add(0,first.getBbHeight()/2,0));if(delta.lengthSqr()>1)delta=delta.normalize();first.setDeltaMovement(delta.scale(3));first.hurtMarked=true;}}
            case BALANCED,GLUTTONY->{}
        }
    }
    @Override public void playerTouch(Player player){
        if(level().isClientSide || isRemoved() || player.isSpectator() || player.takeXpDelay!=0 || player.isInvulnerable() || !canAffect(player))return;
        if(powderType()==PowderType.TAINT)WandInventory.consume(player,ConciliumFocus.vis("aer",10000,"ignis",10000,"aqua",10000,"terra",10000,"ordo",10000,"perditio",10000));
        if(powderType()==PowderType.GLUTTONY)player.getFoodData().eat(1,1);
        player.takeXpDelay=2;discard();
    }
    @Override public boolean hurt(DamageSource source,float damage){
        if(isInvulnerableTo(source))return false;if(!level().isClientSide && Float.isFinite(damage)){health=(int)(health-damage);if(health<=0)discard();}return false;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putInt("PowderAge",age);tag.putInt("Health",health);tag.putInt("Type",powderType().ordinal());tag.putInt("Iteration",iteration);if(ownerId!=null)tag.putUUID("Owner",ownerId);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){age=net.minecraft.util.Mth.clamp(tag.getInt("PowderAge"),0,40);health=tag.contains("Health")?net.minecraft.util.Mth.clamp(tag.getInt("Health"),0,5):5;if(tag.contains("Type"))powderType(PowderType.byId(tag.getInt("Type")));iteration=net.minecraft.util.Mth.clamp(tag.getInt("Iteration"),0,5);ownerId=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;}
    @Override public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buffer){buffer.writeByte(age);}
    @Override public void readSpawnData(net.minecraft.network.FriendlyByteBuf buffer){age=Math.min(40,buffer.readUnsignedByte());}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
