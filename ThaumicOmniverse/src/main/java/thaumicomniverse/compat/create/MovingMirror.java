package thaumicomniverse.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import thaumcraft.api.transport.MirrorEndpoints;

public class MovingMirror implements MovementBehaviour {
    @Override public void tick(MovementContext context){
        if(!ArcaneContraptions.enabled()||!(context.world instanceof ServerLevel level)||context.position==null||context.blockEntityData==null)return;
        if(!(context.temporaryData instanceof ItemMirrorBlockEntity)){
            var mirror=new ItemMirrorBlockEntity(context.localPos,context.state);mirror.load(context.blockEntityData);
            context.temporaryData=mirror;
            mirror.mount(level,new MirrorEndpoints.MobileHost(){
                public Vec3 position(){return context.position;}
                public Vec3 outward(){return ArcaneMovement.vector(context,context.state.getValue(ItemMirrorBlock.FACING));}
                public boolean valid(){return !context.contraption.disassembled&&context.contraption.entity!=null&&!context.contraption.entity.isRemoved()&&ArcaneContraptions.enabled();}
                public void changed(ItemMirrorBlockEntity tile){context.blockEntityData.merge(tile.saveWithoutMetadata());ArcaneMovement.sync(context);}
                public void transfer(){level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,context.position.x,context.position.y,context.position.z,4,.2,.2,.2,.02);}
                public int transferOutput(net.minecraft.world.item.ItemStack offered){
                    var down=ArcaneMovement.vector(context,Direction.DOWN);
                    var travel=Direction.getNearest(down.x,down.y,down.z);
                    if(down.dot(Vec3.atLowerCornerOf(travel.getNormal()))<.999)return -1;
                    var target=net.minecraft.core.BlockPos.containing(context.contraption.entity.toGlobalVector(context.localPos.below().getCenter(),1));
                    var accepted=MirrorBridge.output(level,target,travel,offered);
                    return accepted==null?-1:accepted;
                }
            });
        }
        var mirror=(ItemMirrorBlockEntity)context.temporaryData;mirror.tick();
        var facing=context.state.getValue(ItemMirrorBlock.FACING);
        for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(context.position,context.position).inflate(1.3))){
            if(!item.isAlive()||item.getPortalCooldown()>0)continue;
            var local=context.contraption.entity.toLocalVector(item.position(),1).subtract(Vec3.atLowerCornerOf(context.localPos));
            double x=local.x,y=local.y,z=local.z;
            boolean inside=x>=-.15&&x<=1.15&&y>=-.15&&y<=1.15&&z>=-.15&&z<=1.15;
            double depth=switch(facing){case UP->y;case DOWN->1-y;case EAST->x;case WEST->1-x;case SOUTH->z;case NORTH->1-z;};
            if(inside&&depth<.25)mirror.transport(item);
        }
    }
    @Override public void stopMoving(MovementContext context){if(context.temporaryData instanceof ItemMirrorBlockEntity mirror){context.blockEntityData.merge(mirror.saveWithoutMetadata());mirror.setRemoved();}}
    public static ItemMirrorBlockEntity at(com.simibubi.create.content.contraptions.Contraption structure,net.minecraft.core.BlockPos local){
        for(var actor:structure.getActors())if(actor.left.pos().equals(local)&&actor.right!=null&&actor.right.temporaryData instanceof ItemMirrorBlockEntity mirror)return mirror;
        return null;
    }
}
