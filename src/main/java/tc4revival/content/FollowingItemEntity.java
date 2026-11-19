package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

/** Magical delivery items retain their destination owner through entity saves. */
public  class FollowingItemEntity extends SpecialItemEntity implements IEntityAdditionalSpawnData {
    private Entity target;
    private java.util.UUID targetUuid;
    private int targetId=-1;
    private Vec3 destination=Vec3.ZERO;
    private int approachAge=20;
    private int effect=3;
    public FollowingItemEntity(EntityType<? extends ItemEntity> type,Level level) { super(type,level); }
    public void follow(Entity entity,int effect) { this.target=entity;this.targetUuid=entity.getUUID();this.targetId=entity.getId(); this.effect=effect; refreshTarget(); noPhysics=true; }
    public boolean hasFollowingTarget() { return target!=null; }
    private void refreshTarget() {
        if(target==null && targetUuid!=null && level() instanceof net.minecraft.server.level.ServerLevel server)target=server.getEntity(targetUuid);
        if(target==null && level().isClientSide && targetId>=0)target=level().getEntity(targetId);
        if(target!=null && (!target.isAlive() || target.level()!=level()) || target==null && tickCount>40) {
            target=null;targetUuid=null;targetId=-1;destination=Vec3.ZERO;noPhysics=false;
        }
        if (target!=null) destination=new Vec3(target.getX(),target.getBoundingBox().minY+target.getBbHeight()/2,target.getZ());
    }
    @Override public void tick() {
        refreshTarget();
        if (!destination.equals(Vec3.ZERO)) {
            float x=(float)(destination.x-getX()),y=(float)(destination.y-getY()),z=(float)(destination.z-getZ());
            if (approachAge>1) approachAge--;
            double distance=(float)Math.sqrt(x*x+y*y+z*z);
            if (distance>.5) setDeltaMovement(new Vec3(x,y,z).scale(1/(distance*approachAge)));
            else { setDeltaMovement(getDeltaMovement().scale((double).1f)); destination=Vec3.ZERO; target=null;targetUuid=null;targetId=-1; noPhysics=false; }
            if (level().isClientSide) {
                var point=new Vec3(xo+(random.nextFloat()-random.nextFloat())*.125f,yo+.125f+(random.nextFloat()-random.nextFloat())*.125f,zo+(random.nextFloat()-random.nextFloat())*.125f);
                if (effect==10) tc4revival.client.CrucibleEffects.elementalBubble(level(),point.x,point.y,point.z);
                else tc4revival.client.ClientFocusEffects.followingSparkle(level(),point,effect);
            }
        }
        super.tick();
    }
    @Override public void move(MoverType type,Vec3 velocity) {
        // Magical delivery can pass through blocks; ordinary dropped items retain native collision.
        if (!destination.equals(Vec3.ZERO)) noPhysics=true;
        super.move(type,velocity);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putShort("type",(short)effect);tag.putInt("ApproachAge",approachAge);if(targetUuid!=null)tag.putUUID("FollowTarget",targetUuid); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); effect=tag.getShort("type"); target=null;targetUuid=tag.hasUUID("FollowTarget")?tag.getUUID("FollowTarget"):null; destination=Vec3.ZERO; approachAge=tag.contains("ApproachAge")?Math.max(1,tag.getInt("ApproachAge")):20;
    }
    @Override public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeInt(target==null?-1:target.getId()); buffer.writeDouble(destination.x); buffer.writeDouble(destination.y); buffer.writeDouble(destination.z); buffer.writeByte(effect);
    }
    @Override public void readSpawnData(FriendlyByteBuf buffer) {
        int id=buffer.readInt();targetId=id; target=id<0?null:level().getEntity(id);
        destination=new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble()); effect=buffer.readByte();
    }
}
