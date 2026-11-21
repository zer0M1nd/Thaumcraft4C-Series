package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Old EntityItemGrate: suppress solid-block ejection while inside either grate state. */
public  class GrateItemEntity extends ItemEntity {
    public GrateItemEntity(EntityType<? extends ItemEntity> type,Level level) {super(type,level);}
    private boolean insideGrate(double x,double y,double z) {
        return level().getBlockState(BlockPos.containing(x,y,z)).getBlock() instanceof MetalGrateBlock;
    }
    @Override protected void moveTowardsClosestSpace(double x,double y,double z) {
        if(insideGrate(x,y,z)) {noPhysics=false;return;}
        super.moveTowardsClosestSpace(x,y,z);
    }
    @Override public void move(MoverType type,Vec3 movement) {
        // Ignore the grate's thin plate through its collision shape, never all
        // block collisions: the same movement may cross into a solid floor.
        if(insideGrate(getX(),getBoundingBox().minY+getBbHeight()/2,getZ()))noPhysics=false;
        super.move(type,movement);
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
