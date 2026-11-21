package thaumicomniverse.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.contraptions.gantry.*;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.trains.entity.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.phys.Vec3;
import thaumicomniverse.mixin.CreateFocusLinearAccess;

/** Native controllers retain their axis, stroke, rail path and persistence contracts. */
public final class NativeRelocation {
    public static boolean linear(LinearActuatorBlockEntity tile){
        if(tile.getLevel()==null||tile.getLevel().isClientSide||tile.movedContraption==null)return false;
        var entity=tile.movedContraption;var c=RelocationSessions.control(entity);if(c==null)return false;
        var access=(CreateFocusLinearAccess)tile;
        var unit=access.omniverse$position(tile.offset+1).subtract(access.omniverse$position(tile.offset)).normalize();
        double amount=Mth.clamp(c.delta().dot(unit),-.49,.49);
        float next=Mth.clamp(tile.offset+(float)amount,0,access.omniverse$range());
        var position=access.omniverse$position(next);var delta=position.subtract(entity.position());
        if(!entity.isStalled()&&RelocationSessions.clear(entity,delta)){
            tile.offset=next;entity.setContraptionMotion(delta);entity.setPos(position);
        }else entity.setContraptionMotion(Vec3.ZERO);
        access.omniverse$force(true);tile.sendData();return true;
    }
    public static Float bearing(ControlledContraptionEntity entity){
        if(entity==null||entity.level().isClientSide)return null;
        var c=RelocationSessions.control(entity);if(c==null)return null;
        var axis=entity.getRotationAxis();if(axis==null)return 0f;
        var pivot=entity.getAnchorVec().add(.5,.5,.5);
        var old=c.entity.toGlobalVector(c.grab,1).subtract(pivot);
        var target=c.target().subtract(pivot);
        var normal=Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis,Direction.AxisDirection.POSITIVE).getNormal());
        old=old.subtract(normal.scale(old.dot(normal)));target=target.subtract(normal.scale(target.dot(normal)));
        if(old.lengthSqr()<1e-5||target.lengthSqr()<1e-5)return 0f;
        float delta=(float)Math.toDegrees(Math.atan2(normal.dot(old.cross(target)),old.dot(target)));
        delta=Mth.clamp(delta,-5,5);
        float previous=entity.getAngle(1);
        entity.setAngle(previous+delta);
        boolean clear=RelocationSessions.clear(entity,Vec3.ZERO);
        entity.setAngle(previous);
        return clear?delta:0;
    }
    public static Vec3 gantry(GantryContraptionEntity entity,Direction axis){
        var c=RelocationSessions.control(entity);if(c==null)return null;
        var direction=Vec3.atLowerCornerOf(axis.getNormal());
        var delta=direction.scale(Mth.clamp(c.delta().dot(direction),-.49,.49));
        var facing=((GantryContraption)entity.getContraption()).getFacing();
        var next=BlockPos.containing(entity.getAnchorVec().add(.5,.5,.5).add(delta)).relative(facing.getOpposite());
        var state=entity.level().getBlockState(next);
        if(!state.is(AllBlocks.GANTRY_SHAFT.get())||state.getValue(GantryShaftBlock.FACING).getAxis()!=axis.getAxis()
                ||!RelocationSessions.clear(entity,delta))return Vec3.ZERO;
        return delta;
    }
    public static void train(Train train){
        if(train.graph==null||train.derailed)return;
        for(var carriage:train.carriages){
            final RelocationSessions.Control[] found={null};
            carriage.forEachPresentEntity(e->{var control=RelocationSessions.control(e);if(control!=null)found[0]=control;});
            var c=found[0];if(c==null)continue;
            var tangent=carriage.getLeadingPoint().getPosition(train.graph).subtract(carriage.getTrailingPoint().getPosition(train.graph)).normalize();
            double speed=Mth.clamp(c.delta().dot(tangent),-.3,.3);
            if(!RelocationSessions.clear(c.entity,tangent.scale(speed)))speed=0;
            train.speed=speed;train.targetSpeed=speed;return;
        }
    }
    public static void moveCart(RelocationSessions.Control c,AbstractMinecart cart){
        var level=cart.level();var pos=cart.blockPosition();
        if(BaseRailBlock.isRail(level,pos.below()))pos=pos.below();
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof BaseRailBlock rail)){cart.setDeltaMovement(Vec3.ZERO);return;}
        var shape=rail.getRailDirection(state,level,pos,cart);
        Vec3 tangent=switch(shape){
            case EAST_WEST,ASCENDING_EAST,ASCENDING_WEST->new Vec3(1,0,0);
            case NORTH_SOUTH,ASCENDING_NORTH,ASCENDING_SOUTH->new Vec3(0,0,1);
            case SOUTH_EAST,NORTH_WEST->new Vec3(1,0,-1).normalize();
            default->new Vec3(1,0,1).normalize();
        };
        double amount=Mth.clamp(c.delta().dot(tangent),-.3,.3);
        var delta=tangent.scale(amount);var ahead=BlockPos.containing(cart.position().add(delta.scale(2)));
        boolean track=BaseRailBlock.isRail(level,ahead)||BaseRailBlock.isRail(level,ahead.below())||BaseRailBlock.isRail(level,ahead.above());
        cart.setDeltaMovement(track&&RelocationSessions.clear(c.entity,delta)?delta:Vec3.ZERO);
    }
    private NativeRelocation(){}
}
