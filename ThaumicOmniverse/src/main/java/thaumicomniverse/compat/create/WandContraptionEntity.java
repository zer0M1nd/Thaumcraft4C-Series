package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent free assembly, with opt-in ballistic release from the physics upgrade. */
public class WandContraptionEntity extends OrientedContraptionEntity {
    private static final EntityDataAccessor<Float> TURN=SynchedEntityData.defineId(WandContraptionEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PROJECTILE=SynchedEntityData.defineId(WandContraptionEntity.class,EntityDataSerializers.BOOLEAN);
    private Vec3 clientTarget;
    private int clientSteps;
    private Vec3 alignmentStart,alignmentTarget;
    private float alignmentYawStart,alignmentYawTarget;
    private int alignmentTicks;
    private boolean alignmentPlaces;
    private final RelocationProjectile projectile=new RelocationProjectile(this);
    private final java.util.Map<Integer,CompoundTag> pendingPassengers=new java.util.HashMap<>();
    private final java.util.List<net.minecraft.world.item.ItemStack> returnedItems=new java.util.ArrayList<>();
    public void carryPassenger(int seat,CompoundTag tag){pendingPassengers.put(seat,tag.copy());}
    public void carryItem(net.minecraft.world.item.ItemStack stack){if(!stack.isEmpty())returnedItems.add(stack.copy());}
    private boolean restoreCargo(){
        var level=(ServerLevel)level();
        for(var entry:java.util.List.copyOf(pendingPassengers.entrySet())){
            var tag=entry.getValue();
            if(tag.hasUUID("UUID")){
                Entity existing=null;for(var dimension:level.getServer().getAllLevels()){existing=dimension.getEntity(tag.getUUID("UUID"));if(existing!=null)break;}
                if(existing!=null){pendingPassengers.remove(entry.getKey());continue;}
            }
            var passenger=EntityType.loadEntityRecursive(tag,level,e->{e.moveTo(position());return e;});
            if(passenger==null||!level.tryAddFreshEntityWithPassengers(passenger))continue;
            addSittingPassenger(passenger,entry.getKey());pendingPassengers.remove(entry.getKey());
        }
        for(var stack:java.util.List.copyOf(returnedItems)){
            var item=new net.minecraft.world.entity.item.ItemEntity(level,getX(),getY()+.5,getZ(),stack.copy());
            item.setDefaultPickUpDelay();if(level.addFreshEntity(item))returnedItems.remove(stack);
        }
        return pendingPassengers.isEmpty()&&returnedItems.isEmpty();
    }
    public WandContraptionEntity(EntityType<?> type,Level level){super(type,level);}
    /** Preserve the native pose and its storage serialization, while dropping controller-specific tick code. */
    public static WandContraptionEntity copyForThrow(AbstractContraptionEntity source){
        var data=source.getContraption().writeNBT(false).copy();data.putString("Type","thaumicomniverse:wand_mobile");
        // Trains store cargo in their global carriage ledger, outside the entity NBT.
        if(source instanceof com.simibubi.create.content.trains.entity.CarriageContraptionEntity carriage&&carriage.getCarriage()!=null)
            carriage.getCarriage().storage.write(data,false);
        var next=new WandContraptionEntity(ArcaneCreateContent.WAND_MOBILE.get(),source.level());
        next.setContraption(Contraption.fromNBT(source.level(),data,false));
        if(source.getContraption() instanceof com.simibubi.create.content.contraptions.piston.PistonContraption
                ||source.getContraption() instanceof com.simibubi.create.content.contraptions.mounted.MountedContraption){
            if(source.getContraption() instanceof com.simibubi.create.content.contraptions.piston.PistonContraption)
                next.getContraption().getBlocks().values().removeIf(RelocationTransfer::pistonHardware);
            else next.getContraption().getBlocks().values().removeIf(info->com.simibubi.create.AllBlocks.MINECART_ANCHOR.has(info.state()));
            if(next.getContraption().getBlocks().isEmpty())return null;
            net.minecraft.world.phys.AABB bounds=null;
            for(var pos:next.getContraption().getBlocks().keySet()){var box=new net.minecraft.world.phys.AABB(pos);bounds=bounds==null?box:bounds.minmax(box);}
            next.getContraption().bounds=bounds;next.getContraption().invalidateColliders();
        }
        next.setInitialOrientation(Direction.SOUTH);next.startAtInitialYaw();
        next.setPos(source.getAnchorVec().add(.5,0,.5));
        if(source instanceof OrientedContraptionEntity oriented){
            next.setInitialOrientation(oriented.getInitialOrientation());next.yaw=oriented.yaw;next.pitch=oriented.pitch;
        }else if(source instanceof ControlledContraptionEntity controlled&&controlled.getRotationAxis()!=null){
            float angle=controlled.getAngle(1);
            switch(controlled.getRotationAxis()){
                case Y->next.yaw=-angle;
                case Z->next.pitch=angle;
                case X->{next.setInitialOrientation(Direction.WEST);next.yaw=90;next.pitch=-angle;}
            }
        }
        // Do not detach if a future native implementation changes its anchor/rotation contract.
        for(var sample:java.util.List.of(Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1)))
            if(source.toGlobalVector(sample,1).distanceToSqr(next.toGlobalVector(sample,1))>1e-8)return null;
        next.prevYaw=next.yaw;next.prevPitch=next.pitch;next.entityData.set(TURN,next.yaw);
        return next;
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TURN,0f);entityData.define(PROJECTILE,false);}
    public static WandContraptionEntity assemble(ServerLevel level,BlockPos root,BlockPos a,BlockPos b,
            net.minecraft.world.entity.player.Player player)throws AssemblyException{
        var structure=a==null?new WandMovingStructure():new WandMovingStructure(a,b);
        if(!structure.assemble(level,root))return null;
        for(var local:structure.getBlocks().keySet()){
            var pos=local.offset(structure.anchor);
            if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem()))return null;
        }
        var entity=new WandContraptionEntity(ArcaneCreateContent.WAND_MOBILE.get(),level);
        entity.setInitialOrientation(Direction.SOUTH);entity.startAtInitialYaw();
        entity.setContraption(structure);entity.setPos(root.getX()+.5,root.getY(),root.getZ()+.5);
        structure.removeBlocksFromWorld(level,BlockPos.ZERO);
        if(!level.addFreshEntity(entity)){entity.disassemble();return null;}
        FocusEffects.formation(entity);
        return entity;
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){
        clientTarget=new Vec3(x,y,z);clientSteps=Math.max(1,steps);
        if(teleport){setPos(clientTarget);clientSteps=0;}
    }
    @Override protected void tickContraption(){
        prevYaw=yaw;prevPitch=pitch;
        if(level().isClientSide){
            yaw=net.minecraft.util.Mth.rotLerp(.5f,yaw,entityData.get(TURN));
            if(clientSteps>0){var next=position().lerp(clientTarget,1d/clientSteps--);setContraptionMotion(next.subtract(position()));setPos(next);}
        }else{
            setContraptionMotion(Vec3.ZERO);
            var delta=RelocationSessions.motion(this);
            if(delta!=null){cancelProjectile();cancelAlignment();RelocationSessions.moveFree(this,delta);}
            else if(projectile.active())projectile.tick();
            else if(alignmentTarget!=null)tickAlignment();
            entityData.set(TURN,yaw);
            entityData.set(PROJECTILE,projectile.active());
            if(!isRemoved())RelocationPassengers.get(((ServerLevel)level()).getServer()).update(this);
        }
        tickActors();
    }
    public boolean turn(float degrees){
        cancelAlignment();
        float old=yaw;int steps=Math.max(1,(int)Math.ceil(Math.abs(degrees)/3));
        for(int i=1;i<=steps;i++){
            yaw=old+degrees*i/steps;
            if(!RelocationSessions.clear(this,Vec3.ZERO)){yaw=old;return false;}
        }
        entityData.set(TURN,yaw);return true;
    }
    public boolean placeSafely(){
        cancelProjectile();
        return planAlignment(true);
    }
    public void throwFrom(net.minecraft.world.entity.player.Player player){cancelAlignment();projectile.launch(player);entityData.set(PROJECTILE,true);}
    public void cancelProjectile(){projectile.cancel();if(!level().isClientSide)entityData.set(PROJECTILE,false);}
    public boolean isProjectile(){return level().isClientSide?entityData.get(PROJECTILE):projectile.active();}
    boolean finishProjectilePlacement(){
        if(!restoreCargo())return false;
        RelocationPassengers.get(((ServerLevel)level()).getServer()).update(this);
        FocusEffects.formation(this);disassemble();return true;
    }
    public void alignOnRelease(){if(!level().isClientSide){cancelAlignment();RelocationCollision.release(this);}}
    public void cancelAlignment(){alignmentStart=null;alignmentTarget=null;alignmentTicks=0;alignmentPlaces=false;}
    private boolean placementSpace(){return placementSpace(null);}
    java.util.List<AbstractContraptionEntity> movingParts(){
        var result=new java.util.ArrayList<AbstractContraptionEntity>();collectParts(this,result);return result;
    }
    private static void collectParts(AbstractContraptionEntity entity,java.util.List<AbstractContraptionEntity> result){
        result.add(entity);for(var passenger:entity.getPassengers())if(passenger instanceof AbstractContraptionEntity child){entity.positionRider(child);collectParts(child,result);}
    }
    boolean placementSpace(net.minecraft.world.entity.player.Player player){
        var occupied=new java.util.HashSet<BlockPos>();
        for(var part:movingParts()){
        var transform=part==this?makeStructureTransform():new StructureTransform(BlockPos.containing(part.getAnchorVec().add(.5,.5,.5)),0,
                part instanceof OrientedContraptionEntity oriented?-oriented.yaw+oriented.getInitialYaw():0,0);
        for(var info:part.getContraption().getBlocks().values()){
            var at=transform.apply(info.pos());
            if(!occupied.add(at)||!level().hasChunkAt(at)||level().isOutsideBuildHeight(at)||!level().getWorldBorder().isWithinBounds(at)
                    ||!level().getBlockState(at).canBeReplaced()||level().getBlockEntity(at)!=null
                    ||player!=null&&(!level().mayInteract(player,at)||!player.mayUseItemAt(at,Direction.UP,player.getMainHandItem())))return false;
            if(!level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(at).deflate(1e-5),
                    e->e.isAlive()&&!e.isSpectator()&&!e.isPassengerOfSameVehicle(this)).isEmpty())return false;
        }
        }
        return true;
    }
    private boolean planAlignment(boolean place){
        if(level().isClientSide||getContraption()==null)return false;
        cancelAlignment();var start=position();float initial=yaw;
        float targetYaw=place?initial+net.minecraft.util.Mth.wrapDegrees(Math.round(initial/90)*90-initial):initial;
        var nearest=Vec3.atBottomCenterOf(BlockPos.containing(getAnchorVec().add(.5,.5,.5)));
        var candidates=new java.util.ArrayList<Vec3>();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var candidate=nearest.add(x,y,z);if(candidate.distanceToSqr(start)<=.75*.75)candidates.add(candidate);
        }
        candidates.sort(java.util.Comparator.comparingDouble(start::distanceToSqr));
        try{
            for(var target:candidates){
                setPos(target);yaw=targetYaw;
                if(!RelocationSessions.clear(this,Vec3.ZERO,1e-5)||place&&!placementSpace())continue;
                boolean path=true;
                int steps=Math.max(1,Math.max((int)Math.ceil(start.distanceTo(target)*32),(int)Math.ceil(Math.abs(targetYaw-initial)/2)));
                for(int step=1;step<=steps;step++){
                    double t=step/(double)steps;setPos(start.lerp(target,t));yaw=net.minecraft.util.Mth.lerp((float)t,initial,targetYaw);
                    if(!RelocationSessions.clear(this,Vec3.ZERO)){path=false;break;}
                }
                if(!path)continue;
                alignmentStart=start;alignmentTarget=target;alignmentYawStart=initial;alignmentYawTarget=targetYaw;
                alignmentTicks=0;alignmentPlaces=place;return true;
            }
            return false;
        }finally{setPos(start);yaw=initial;}
    }
    private void tickAlignment(){
        double t=Math.min(1,++alignmentTicks/8d);t=t*t*(3-2*t);
        var before=position();float beforeYaw=yaw;
        var next=alignmentStart.lerp(alignmentTarget,t);yaw=net.minecraft.util.Mth.lerp((float)t,alignmentYawStart,alignmentYawTarget);
        setPos(next);
        if(!RelocationSessions.clear(this,Vec3.ZERO,alignmentTicks>=8?1e-5:1d/32)){
            setPos(before);yaw=beforeYaw;cancelAlignment();setContraptionMotion(Vec3.ZERO);return;
        }
        setContraptionMotion(next.subtract(before));
        if(alignmentTicks>=8){
            boolean place=alignmentPlaces;cancelAlignment();
            if(place&&placementSpace())finishProjectilePlacement();
        }
    }
    @Override protected void readAdditional(CompoundTag tag,boolean spawn){
        super.readAdditional(tag,spawn);pitch=tag.getFloat("Pitch");prevPitch=pitch;entityData.set(TURN,yaw);projectile.read(tag);entityData.set(PROJECTILE,projectile.active());
        pendingPassengers.clear();for(var element:tag.getList("RelocationPassengers",10)){var row=(CompoundTag)element;pendingPassengers.put(row.getInt("Seat"),row.getCompound("Entity").copy());}
        returnedItems.clear();for(var element:tag.getList("RelocationReturns",10))returnedItems.add(net.minecraft.world.item.ItemStack.of((CompoundTag)element));
    }
    @Override protected void writeAdditional(CompoundTag tag,boolean spawn){
        super.writeAdditional(tag,spawn);tag.putFloat("Pitch",pitch);projectile.write(tag);
        var passengers=new net.minecraft.nbt.ListTag();pendingPassengers.forEach((seat,entity)->{var row=new CompoundTag();row.putInt("Seat",seat);row.put("Entity",entity.copy());passengers.add(row);});tag.put("RelocationPassengers",passengers);
        var items=new net.minecraft.nbt.ListTag();returnedItems.forEach(stack->items.add(stack.save(new CompoundTag())));tag.put("RelocationReturns",items);
    }
}
