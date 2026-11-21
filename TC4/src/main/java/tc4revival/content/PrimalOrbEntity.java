package tc4revival.content;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

public  class PrimalOrbEntity extends ThrowableProjectile implements IEntityAdditionalSpawnData {
    private int count,ownerId;
    private boolean seeker,impacted;
    public PrimalOrbEntity(EntityType<? extends PrimalOrbEntity> type,Level level) { super(type,level); }
    public PrimalOrbEntity(Level level,LivingEntity owner,boolean seeker) {
        this(TCEntities.PRIMAL_ORB.get(),level); this.seeker=seeker; ownerId=owner.getId(); setOwner(owner);
        float yaw=owner.getYRot()*Mth.DEG_TO_RAD;
        setPos(owner.getX()-Mth.cos(yaw)*.16,owner.getEyeY()-.1,owner.getZ()-Mth.sin(yaw)*.16);
        setYRot(owner.getYRot()); setXRot(owner.getXRot());
        var look=owner.getLookAngle().normalize();
        setDeltaMovement(look.add(random.nextGaussian()*.0075,random.nextGaussian()*.0075,random.nextGaussian()*.0075).scale(.5));
    }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return .001f; }
    private boolean inPortal() {
        var state=level().getBlockState(BlockPos.containing(getX(),getEyeY(),getZ()));
        return state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL);
    }
    @Override protected boolean canHitEntity(Entity entity) {
        return entity.isPickable() && !entity.isSpectator() && (entity!=getOwner() || tickCount>=5);
    }
    @Override public void tick() {
        count++;
        if (inPortal()) onHit(new EntityHitResult(this));
        if (isRemoved()) return;
        if (level().isClientSide) tc4revival.client.PrimalOrbEffects.trail(this);
        if (tickCount>20) {
            if (!seeker) {
                var roll=new Random(getId()+count);
                setDeltaMovement(getDeltaMovement().add((roll.nextFloat()-roll.nextFloat())*.01f,
                        (roll.nextFloat()-roll.nextFloat())*.01f,(roll.nextFloat()-roll.nextFloat())*.01f));
            } else {
                LivingEntity chosen=null; double distance=Double.MAX_VALUE;
                for (var entity:level().getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(16))) {
                    double d=distanceToSqr(entity);
                    if (entity!=getOwner() && entity.isAlive() && !entity.isSpectator() && (getOwner()==null || !getOwner().isAlliedTo(entity)) && d<distance) { distance=d; chosen=entity; }
                }
                if (chosen!=null && distance>1.0e-12) {
                    var toward=new Vec3(chosen.getX()-getX(),chosen.getBoundingBox().minY+chosen.getBbHeight()*.9-getY(),chosen.getZ()-getZ()).normalize().scale(.2);
                    var motion=getDeltaMovement().add(toward);
                    setDeltaMovement(Mth.clamp((float)motion.x,-.2f,.2f),Mth.clamp((float)motion.y,-.2f,.2f),Mth.clamp((float)motion.z,-.2f,.2f));
                }
            }
        }
        super.tick(); if (tickCount>5000) discard();
    }
    @Override protected void onHit(HitResult hit) {
        if (impacted || isRemoved()) return;
        if (level().isClientSide) { impacted=true; tc4revival.client.PrimalOrbEffects.impact(this); return; }
        impacted=true;
        boolean portal=inPortal();
        level().explode(this,getX(),getY(),getZ(),portal ? 4 : 2,false,Level.ExplosionInteraction.BLOCK);
        if (!seeker && random.nextInt(100)<=(portal ? 10 : 1)) {
            if (random.nextBoolean()) taintBurst((ServerLevel)level());
            else AuraNodeGeneration.createRandom((ServerLevel)level(),hit instanceof BlockHitResult block ? block.getBlockPos() : blockPosition(),random,false,false,true);
        }
        level().broadcastEntityEvent(this,(byte)62); discard();
    }
    private void taintBurst(ServerLevel level) {
        int originX=Mth.floor(getX()),originZ=Mth.floor(getZ());
        for (int i=0;i<10;i++) {
            int x=originX+random.nextInt(7)-random.nextInt(7),z=originZ+random.nextInt(7)-random.nextInt(7);
            var column=new BlockPos(x,Mth.floor(getY()),z);
            if (!random.nextBoolean() || !level.hasChunkAt(column) || TCBiomes.isTainted(level,column)) continue;
            WorldBiomeChanges.changeColumn(level,column,TCBiomes.TAINTED_LAND);
            var top=level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,column);
            if (!level.isOutsideBuildHeight(top) && !level.isEmptyBlock(top.below()) && level.getBlockEntity(top)==null)
                level.setBlock(top,TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),level,top),3);
        }
    }
    @Override public void handleEntityEvent(byte event) {
        if (event==62) { if (!impacted) tc4revival.client.PrimalOrbEffects.impact(this); impacted=true; }
        else super.handleEntityEvent(event);
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putBoolean("Seeker",seeker);tag.putInt("SteeringAge",count);tag.putInt("FlightAge",tickCount); }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.readAdditionalSaveData(tag);seeker=tag.getBoolean("Seeker");count=Math.max(0,tag.getInt("SteeringAge"));tickCount=Math.max(0,tag.getInt("FlightAge")); }
    @Override public void writeSpawnData(FriendlyByteBuf buffer) { buffer.writeBoolean(seeker); buffer.writeInt(ownerId); }
    @Override public void readSpawnData(FriendlyByteBuf buffer) { seeker=buffer.readBoolean(); ownerId=buffer.readInt(); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
