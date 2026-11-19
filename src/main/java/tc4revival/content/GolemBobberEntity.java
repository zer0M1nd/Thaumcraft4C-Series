package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

/** Non-player float using actual tagged fluid surfaces, independent of the core's catch probability. */
public  class GolemBobberEntity extends Entity implements IEntityAdditionalSpawnData {
    private LivingEntity fisher;
    private int fisherId=-1;
    private boolean inBlock;
    public GolemBobberEntity(EntityType<? extends GolemBobberEntity> type,Level level) { super(type,level); noCulling=true; }
    public void cast(LivingEntity owner,BlockPos target) {
        fisher=owner; fisherId=owner.getId(); setPos(owner.position());
        double x=target.getX()+.5-owner.getX(),y=target.getY()+1-owner.getY(),z=target.getZ()+.5-owner.getZ();
        double distance=(float)Math.sqrt(x*x+y*y+z*z);
        setDeltaMovement(x*.1,y*.1+(float)Math.sqrt(distance)*.08,z*.1);
    }
    public LivingEntity fisher() {
        if (fisher==null && level().getEntity(fisherId) instanceof LivingEntity living) fisher=living;
        return fisher;
    }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public void writeSpawnData(FriendlyByteBuf data) {
        var motion=getDeltaMovement(); data.writeDouble(motion.x); data.writeDouble(motion.y); data.writeDouble(motion.z); data.writeInt(fisherId);
    }
    @Override public void readSpawnData(FriendlyByteBuf data) { setDeltaMovement(data.readDouble(),data.readDouble(),data.readDouble()); fisherId=data.readInt(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double squared) { double range=getBoundingBox().getSize()*256; return squared<range*range; }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (fisher()==null || !fisher().isAlive()) { discard(); return; }
            if (random.nextFloat()<.02f) server.sendParticles(ParticleTypes.SPLASH,getX()+random.nextFloat()-random.nextFloat(),getY()+random.nextFloat(),getZ()+random.nextFloat()-random.nextFloat(),2+random.nextInt(2),.1f,0,.1f,0);
        }
        if (tickCount>4000) { discard(); return; }
        if (inBlock) {
            inBlock=false;
            setDeltaMovement(getDeltaMovement().multiply(random.nextFloat()*.2f,random.nextFloat()*.2f,random.nextFloat()*.2f));
        }
        var motion=getDeltaMovement();
        var hit=level().clip(new ClipContext(position(),position().add(motion),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if (hit.getType()!=HitResult.Type.MISS) {
            inBlock=true;
            if (!level().getBlockState(hit.getBlockPos()).is(Blocks.WATER)) discard();
        }
        if (inBlock) return;
        move(MoverType.SELF,motion);
        float yaw=(float)(Math.atan2(motion.x,motion.z)*180/Math.PI);
        float pitch=(float)(Math.atan2(motion.y,(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z))*180/Math.PI);
        while (pitch-xRotO< -180) xRotO-=360;
        while (pitch-xRotO>=180) xRotO+=360;
        while (yaw-yRotO< -180) yRotO-=360;
        while (yaw-yRotO>=180) yRotO+=360;
        setXRot(xRotO+(pitch-xRotO)*.2f); setYRot(yRotO+(yaw-yRotO)*.2f);
        float drag=onGround() || horizontalCollision?.5f:.92f;
        double submerged=0; var box=getBoundingBox();
        for (int slice=0;slice<5;slice++) {
            var part=new AABB(box.minX,box.minY+box.getYsize()*slice/5,box.minZ,box.maxX,box.minY+box.getYsize()*(slice+1)/5,box.maxZ);
            if (water(part)) submerged+=.2;
        }
        motion=getDeltaMovement().add(0,(double).04f*(submerged*2-1),0);
        if (submerged>0) { drag=(float)(drag*.9); motion=motion.multiply(1,.8,1); }
        setDeltaMovement(motion.scale(drag));
    }
    private boolean water(AABB box) {
        for (var pos:BlockPos.betweenClosed(Mth.floor(box.minX),Mth.floor(box.minY),Mth.floor(box.minZ),Mth.floor(box.maxX),Mth.floor(box.maxY),Mth.floor(box.maxZ)))
            if (level().hasChunkAt(pos)) {
                var fluid=level().getFluidState(pos);
                if (fluid.is(net.minecraft.tags.FluidTags.WATER)
                        && box.minY<pos.getY()+fluid.getHeight(level(),pos) && box.maxY>pos.getY()) return true;
            }
        return false;
    }
}
