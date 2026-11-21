package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import tc4revival.content.*;
import thaumicomniverse.mixin.CreateFocusControllerAccess;
import thaumicomniverse.compat.create.airship.*;

public final class RelocationSessions {
    public record Target(AbstractContraptionEntity entity,Vec3 world,BlockPos local){}
    private static final Map<UUID,Control> CONTROLS=new HashMap<>();
    public static final class Control{
        final Player player;final InteractionHand hand;final ItemStack wand;final AbstractContraptionEntity entity;
        Vec3 offset;
        final Vec3 grab;
        long chargedTick;
        long awaitingRelease=-1;
        Control(Player player,InteractionHand hand,ItemStack wand,Target target){
            this.player=player;this.hand=hand;this.wand=wand;entity=target.entity;grab=entity.toLocalVector(target.world,1);
            var delta=target.world.subtract(player.getEyePosition());
            var forward=player.getLookAngle();var right=right(player);var up=right.cross(forward).normalize();
            offset=new Vec3(delta.dot(right),delta.dot(up),delta.dot(forward));chargedTick=player.level().getGameTime();
        }
        Vec3 target(){
            var forward=player.getLookAngle();var right=right(player);var up=right.cross(forward).normalize();
            return player.getEyePosition().add(right.scale(offset.x)).add(up.scale(offset.y)).add(forward.scale(offset.z));
        }
        Vec3 delta(){return target().subtract(entity.toGlobalVector(grab,1));}
    }
    private static Vec3 right(Player player){double yaw=Math.toRadians(player.getYRot());return new Vec3(Math.cos(yaw),0,Math.sin(yaw));}
    public static Target target(Player player){
        Vec3 eye=player.getEyePosition(),end=eye.add(player.getLookAngle().scale(16));
        var wall=player.level().clip(new ClipContext(eye,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        double distance=wall.getType()==HitResult.Type.BLOCK?eye.distanceToSqr(wall.getLocation()):256;
        Target best=null;
        for(var ref:ContraptionHandler.loadedContraptions.get(player.level()).values()){
            var entity=ref.get();
            if(entity==null||entity.isRemoved()||entity.getContraption()==null)continue;
            var hit=ArcaneContraptions.ray(entity,eye,end).orElse(null);
            if(hit!=null&&eye.distanceToSqr(hit.world())<=distance+.001){
                distance=eye.distanceToSqr(hit.world());best=new Target(entity,hit.world(),hit.local());
            }
        }
        return best;
    }
    public static boolean begin(Player player,InteractionHand hand,ItemStack wand){
        var target=target(player);
        if(target==null && player.level() instanceof ServerLevel level){
            var eye=player.getEyePosition();
            var hit=level.clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(16)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
            if(hit.getType()!=HitResult.Type.BLOCK||!level.mayInteract(player,hit.getBlockPos()))return false;
            try{
                var created=WandContraptionEntity.assemble(level,hit.getBlockPos(),null,null,player);
                if(created==null)return false;
                target=new Target(created,hit.getLocation(),BlockPos.ZERO);
            }catch(AssemblyException failure){player.displayClientMessage(failure.component,true);return false;}
        }
        if(target==null)return false;
        var entity=target.entity;
        if(!(entity instanceof WandContraptionEntity||entity instanceof AirshipEntity||entity instanceof GantryContraptionEntity
                ||entity instanceof CarriageContraptionEntity||entity.getVehicle() instanceof AbstractMinecart
                ||entity instanceof ControlledContraptionEntity))return false;
        if(CONTROLS.values().stream().anyMatch(c->c.entity==entity&&c.player!=player))return false;
        release(player);
        CONTROLS.put(player.getUUID(),new Control(player,hand,wand,target));
        if(!charge(player,wand))return false;
        if(entity instanceof WandContraptionEntity free){free.cancelProjectile();free.cancelAlignment();}
        if(entity instanceof AirshipEntity airship)airship.detachForRelocation();
        return true;
    }
    public static boolean charge(Player player,ItemStack wand){
        var c=CONTROLS.get(player.getUUID());
        if(c==null||c.wand!=wand||!valid(c)){release(player);return false;}
        var focus=WandFocusItem.attached(wand);
        if(!(focus.getItem() instanceof RelocationFocusItem item)
                ||!((CastingWandItem)wand.getItem()).consumeForPlayer(wand,player,item.costs(focus),false,false,true)){
            release(player);return false;
        }
        c.chargedTick=player.level().getGameTime();return true;
    }
    private static boolean valid(Control c){
        return ArcaneContraptions.enabled()&&c.player.isAlive()&&!c.player.isSpectator()
                &&c.player.level()==c.entity.level()&&!c.entity.isRemoved()
                &&c.player.getItemInHand(c.hand)==c.wand
                &&c.player.getEyePosition().distanceToSqr(c.entity.toGlobalVector(c.grab,1))<1024;
    }
    public static Control control(AbstractContraptionEntity entity){
        for(var c:CONTROLS.values())if(c.entity==entity&&valid(c)
                &&c.awaitingRelease<0&&entity.level().getGameTime()-c.chargedTick<=1)return c;
        return null;
    }
    public static Vec3 motion(AbstractContraptionEntity entity){
        var c=control(entity);if(c==null)return null;
        var delta=c.delta();return delta.length()>.5?delta.normalize().scale(.5):delta;
    }
    public static void release(Player player){
        var c=CONTROLS.remove(player.getUUID());if(c==null)return;
        c.entity.setContraptionMotion(Vec3.ZERO);
        if(c.entity instanceof WandContraptionEntity free&&!free.isRemoved())free.alignOnRelease();
        if(c.entity instanceof AirshipEntity airship){RelocationCollision.release(airship);airship.relocationPose();}
        if(c.entity.getVehicle() instanceof AbstractMinecart cart)cart.setDeltaMovement(Vec3.ZERO);
        if(c.entity instanceof CarriageContraptionEntity carriage&&carriage.getCarriage()!=null){
            var train=carriage.getCarriage().train;train.speed=0;train.targetSpeed=0;
        }
    }
    /** Only an actual release packet may throw. Invalid sessions, logout and exhausted vis cancel. */
    public static void awaitReleasePacket(Player player){
        var c=CONTROLS.get(player.getUUID());if(c!=null){c.awaitingRelease=player.level().getGameTime();c.entity.setContraptionMotion(Vec3.ZERO);}
    }
    public static void releaseByPlayer(Player player){
        var c=CONTROLS.get(player.getUUID());
        boolean launch=c!=null&&valid(c)&&RelocationFocusItem.physics(WandFocusItem.attached(c.wand));
        release(player);
        if(launch){
            var free=c.entity instanceof WandContraptionEntity original?original:RelocationTransfer.detach(player,c.entity);
            if(free!=null&&!free.isRemoved())free.throwFrom(player);
            else message(player,"blocked");
        }
    }
    public static void turn(Player player,int sign){
        var c=CONTROLS.get(player.getUUID());
        if(c!=null&&valid(c)){if(c.entity instanceof WandContraptionEntity free)free.turn(sign*15);else if(c.entity instanceof AirshipEntity airship)airship.turn(sign*15);}
    }
    public static void distance(Player player,int sign){
        var c=CONTROLS.get(player.getUUID());
        if(c!=null&&valid(c))c.offset=new Vec3(c.offset.x,c.offset.y,
                net.minecraft.util.Mth.clamp(c.offset.z+sign*.75,1.5,24));
    }
    public static void attack(Player player,InteractionHand hand){
        if(!(player.level() instanceof ServerLevel level)||player.isSpectator()||!ArcaneContraptions.enabled())return;
        var wand=player.getItemInHand(hand);var focus=WandFocusItem.attached(wand);
        if(!(focus.getItem() instanceof RelocationFocusItem))return;
        var entity=target(player);
        if(entity!=null){
            if(entity.entity instanceof WandContraptionEntity free){release(player);if(!free.placeSafely())message(player,"blocked");}
            return;
        }
        var eye=player.getEyePosition();
        var hit=level.clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(16)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK||!level.mayInteract(player,hit.getBlockPos()))return;
        var tag=focus.getOrCreateTag();BlockPos a=null,b=null;
        if(tag.contains("RelocationCorner")&&tag.getString("RelocationDimension").equals(level.dimension().location().toString())){
            a=BlockPos.of(tag.getLong("RelocationCorner"));b=hit.getBlockPos();
            int size=RelocationFocusItem.boxSize(focus);
            if(Math.abs(a.getX()-b.getX())>=size||Math.abs(a.getY()-b.getY())>=size||Math.abs(a.getZ()-b.getZ())>=size){
                player.displayClientMessage(Component.translatable("thaumicomniverse.focus.selection_size",size),true);return;
            }
            var min=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));
            b=new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));a=min;
        }else if(player.isShiftKeyDown()&&RelocationFocusItem.architect(focus)){
            tag.putLong("RelocationCorner",hit.getBlockPos().asLong());
            tag.putString("RelocationDimension",level.dimension().location().toString());WandFocusItem.attach(wand,focus);
            message(player,"selected");return;
        }
        try{
            var created=WandContraptionEntity.assemble(level,a==null?hit.getBlockPos():a,a,b,player);
            if(created==null){message(player,"blocked");return;}
            tag.remove("RelocationCorner");tag.remove("RelocationDimension");WandFocusItem.attach(wand,focus);
        }catch(AssemblyException failure){player.displayClientMessage(failure.component,true);}
    }
    private static void message(Player player,String key){player.displayClientMessage(Component.translatable("thaumicomniverse.focus."+key),true);}
    public static boolean clear(AbstractContraptionEntity entity,Vec3 delta){
        return clear(entity,delta,1d/32);
    }
    public static boolean clear(AbstractContraptionEntity entity,Vec3 delta,double tolerance){
        return clearPart(entity,delta,tolerance)&&entity.getPassengers().stream()
                .filter(AbstractContraptionEntity.class::isInstance).map(AbstractContraptionEntity.class::cast)
                .allMatch(child->{entity.positionRider(child);return clear(child,delta,tolerance);});
    }
    private static boolean clearPart(AbstractContraptionEntity entity,Vec3 delta,double tolerance){
        var level=entity.level();var contraption=entity.getContraption();
        var world=new ContraptionWorld(level,contraption);
        for(var info:contraption.getBlocks().values()){
            var shape=info.state().getCollisionShape(world,info.pos());
            for(var box:shape.toAabbs()){
                var oriented=RelocationCollision.transformed(entity,info.pos(),box,tolerance).move(delta);
                var transformed=oriented.bounds();
                var min=BlockPos.containing(transformed.minX,transformed.minY,transformed.minZ);
                var max=BlockPos.containing(transformed.maxX-1e-5,transformed.maxY-1e-5,transformed.maxZ-1e-5);
                if(level.isOutsideBuildHeight(min)||level.isOutsideBuildHeight(max)||!level.hasChunksAt(min,max))return false;
                if(!level.getWorldBorder().isWithinBounds(transformed))return false;
                for(var collision:level.getBlockCollisions(entity,transformed)){
                    if(collision.isEmpty())continue;
                    // A piston rod is intentionally threaded through its stationary base.
                    // Exempt only that base/rod pair; payloads and other obstacles still collide.
                    if(info.state().is(com.simibubi.create.AllBlocks.PISTON_EXTENSION_POLE.get())
                            &&entity instanceof ControlledContraptionEntity controlled
                            &&((CreateFocusControllerAccess)controlled).omniverse$controller()
                                instanceof com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity piston
                            &&new AABB(piston.getBlockPos()).inflate(.001).contains(collision.bounds().getCenter())
                            &&new AABB(piston.getBlockPos()).inflate(.001).minmax(collision.bounds())
                                .equals(new AABB(piston.getBlockPos()).inflate(.001)))continue;
                    for(var obstacle:collision.toAabbs())if(RelocationCollision.overlaps(oriented,RelocationCollision.axisAligned(obstacle)))return false;
                }
                for(var other:level.getEntitiesOfClass(AbstractContraptionEntity.class,transformed,
                        other->other!=entity&&!other.isPassengerOfSameVehicle(entity)&&!other.isRemoved()&&other.getContraption()!=null)){
                    var otherWorld=new ContraptionWorld(level,other.getContraption());
                    for(var block:other.getContraption().getBlocks().values())for(var otherBox:block.state().getCollisionShape(otherWorld,block.pos()).toAabbs())
                        if(RelocationCollision.overlaps(oriented,RelocationCollision.transformed(other,block.pos(),otherBox,tolerance)))return false;
                }
            }
        }
        return true;
    }
    public static void moveFree(AbstractContraptionEntity entity,Vec3 delta){
        Vec3 moved=Vec3.ZERO;int steps=Math.max(1,(int)Math.ceil(delta.length()*8));
        for(int i=0;i<steps;i++){
            var step=delta.scale(1d/steps);
            // Clip each axis independently. Ground contact must not cancel sideways or upward motion.
            var axes=new Vec3[]{new Vec3(0,step.y,0),new Vec3(step.x,0,0),new Vec3(0,0,step.z)};
            if(Math.abs(step.z)>Math.abs(step.x)){var swap=axes[1];axes[1]=axes[2];axes[2]=swap;}
            for(var axis:axes){
                if(axis.lengthSqr()<1e-12)continue;
                double fraction=1;
                if(!clear(entity,axis)){
                    double low=0,high=1;
                    for(int j=0;j<9;j++){double mid=(low+high)/2;if(clear(entity,axis.scale(mid)))low=mid;else high=mid;}
                    fraction=low;
                }
                var actual=axis.scale(fraction);entity.setPos(entity.position().add(actual));moved=moved.add(actual);
            }
        }
        entity.setContraptionMotion(moved);
    }
    public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var c:List.copyOf(CONTROLS.values())){
            if(!valid(c)){release(c.player);continue;}
            if(!c.player.isUsingItem()){
                if(c.awaitingRelease<0)c.awaitingRelease=c.entity.level().getGameTime();
                if(c.entity.level().getGameTime()-c.awaitingRelease>=3)release(c.player);
                continue;
            }
            if(c.entity.level().getGameTime()-c.chargedTick>1){release(c.player);continue;}
            if(c.entity.getVehicle() instanceof AbstractMinecart cart)NativeRelocation.moveCart(c,cart);
            FocusEffects.outline(c.entity);
        }
    }
    private RelocationSessions(){}
}
