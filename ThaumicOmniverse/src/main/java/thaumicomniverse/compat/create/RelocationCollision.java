package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;

/** Local voxel boxes remain oriented; their broad-phase bounds are never collision geometry. */
public final class RelocationCollision extends RelocationGeometry {
    public static Box transformed(AbstractContraptionEntity entity,BlockPos pos,AABB box,double tolerance){
        double inset=Math.min(tolerance,Math.min(box.getXsize(),Math.min(box.getYsize(),box.getZsize()))*.49);
        box=box.deflate(inset);
        Vec3 base=entity.toGlobalVector(Vec3.atLowerCornerOf(pos),1);
        Vec3[] axes=new Vec3[3];for(int i=0;i<3;i++)axes[i]=entity.toGlobalVector(Vec3.atLowerCornerOf(pos).add(WORLD[i]),1).subtract(base).normalize();
        Vec3 center=entity.toGlobalVector(Vec3.atLowerCornerOf(pos).add(box.getCenter()),1);
        double[] half={box.getXsize()/2,box.getYsize()/2,box.getZsize()/2};
        Vec3 ext=Vec3.ZERO;for(int i=0;i<3;i++)ext=ext.add(Math.abs(axes[i].x)*half[i],Math.abs(axes[i].y)*half[i],Math.abs(axes[i].z)*half[i]);
        return new Box(center,axes,half,new AABB(center.subtract(ext),center.add(ext)));
    }
    private static List<Box> boxes(AbstractContraptionEntity entity,double tolerance){
        var out=new ArrayList<Box>();var world=new ContraptionWorld(entity.level(),entity.getContraption());
        for(var info:entity.getContraption().getBlocks().values())for(var box:info.state().getCollisionShape(world,info.pos()).toAabbs())out.add(transformed(entity,info.pos(),box,tolerance));
        return out;
    }
    private static List<Contact> contacts(AbstractContraptionEntity entity,List<Box> boxes,Vec3 offset){
        var out=new ArrayList<Contact>();var level=entity.level();
        for(var original:boxes){var box=original.move(offset);
            for(var shape:level.getBlockCollisions(entity,box.bounds()))for(var obstacle:shape.toAabbs()){
                var other=axisAligned(obstacle);if(overlaps(box,other))out.add(new Contact(box,other));
            }
        }return out;
    }
    public static Vec3 correction(AbstractContraptionEntity entity){
        var geometry=boxes(entity,0);
        return solve(offset->{var hits=contacts(entity,geometry,offset);return hits.isEmpty()?null:hits.get(0);});
    }
    public static void release(AbstractContraptionEntity entity){
        var correction=correction(entity);if(correction.lengthSqr()==0)return;
        if(RelocationSessions.clear(entity,correction,0)){entity.setPos(entity.position().add(correction));entity.setContraptionMotion(correction);}
    }
    private RelocationCollision(){}
}
