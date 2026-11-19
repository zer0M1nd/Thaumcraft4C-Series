package thaumicomniverse.compat.create;

import com.jozufozu.flywheel.util.box.GridAlignedBB;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.NonStationaryLighter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.*;

/** Native light-volume updates, covering the actual interpolated, rotated structure. */
@OnlyIn(Dist.CLIENT)
public final class LegacyMovingLighter extends NonStationaryLighter<Contraption> {
    public LegacyMovingLighter(Contraption structure) { super(structure); }

    @Override public GridAlignedBB getContraptionBounds() {
        var box=contraption.bounds;
        if(box==null)return GridAlignedBB.from(contraption.entity.getBoundingBox());
        AABB world=null;
        for(float partial:new float[]{0,1})for(int i=0;i<8;i++) {
            var local=new Vec3((i&1)==0?box.minX:box.maxX,
                    (i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ);
            var point=contraption.entity.toGlobalVector(local,partial);
            var corner=new AABB(point,point);
            world=world==null?corner:world.minmax(corner);
        }
        return GridAlignedBB.from(world);
    }
}
