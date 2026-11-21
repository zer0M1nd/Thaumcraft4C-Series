package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;

/** A server-owned ballistic transaction; collision never drops or overwrites its cargo. */
final class RelocationProjectile {
    private final WandContraptionEntity entity;
    private Vec3 velocity=Vec3.ZERO;
    private float spin;
    private boolean active,landing;
    private int ticks;
    private UUID owner;
    private String ownerName="Relocation";
    private double mass=-1;
    private Vec3 safePosition;
    private float safeYaw;
    private final Set<UUID> struck=new HashSet<>();

    RelocationProjectile(WandContraptionEntity entity){this.entity=entity;}
    boolean active(){return active;}
    void cancel(){active=landing=false;velocity=Vec3.ZERO;struck.clear();}
    void launch(Player player){
        owner=player.getUUID();ownerName=player.getGameProfile().getName();
        velocity=player.getLookAngle().scale(1.1).add(0,.12,0).add(player.getDeltaMovement().scale(.25));
        spin=(.4f+entity.level().random.nextFloat()*.8f)*(entity.level().random.nextBoolean()?1:-1);
        ticks=0;active=true;landing=false;mass=-1;struck.clear();safePosition=null;
        rememberLanding();
    }
    private Player owner(){
        var level=(ServerLevel)entity.level();var player=owner==null?null:level.getPlayerByUUID(owner);
        return player!=null?player:owner==null?null:FakePlayerFactory.get(level,new GameProfile(owner,ownerName));
    }
    private boolean placementAllowed(){
        var player=owner();if(player==null)return false;
        return entity.placementSpace(player);
    }
    /** Try nearby grid positions before falling back to the last safe pose along the flight. */
    private boolean gridPose(boolean place){
        Vec3 before=entity.position();float yaw=entity.yaw,pitch=entity.pitch;
        float aligned=yaw+Mth.wrapDegrees(Math.round(yaw/90)*90-yaw);
        var nearest=Vec3.atBottomCenterOf(BlockPos.containing(entity.getAnchorVec().add(.5,.5,.5)));
        var candidates=new ArrayList<Vec3>();
        int radius=place?2:1;
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++)
            candidates.add(nearest.add(x,y,z));
        candidates.sort(Comparator.comparingDouble(before::distanceToSqr));
        try{
            for(var candidate:candidates){
                entity.setPos(candidate);entity.yaw=aligned;entity.pitch=0;
                if(!placementAllowed())continue;
                boolean reachable=true;
                int steps=Math.max(1,Math.max((int)Math.ceil(before.distanceTo(candidate)*16),(int)Math.ceil(Math.max(Math.abs(aligned-yaw),Math.abs(pitch))/2)));
                for(int i=1;i<=steps;i++){
                    double fraction=i/(double)steps;entity.setPos(before.lerp(candidate,fraction));entity.yaw=Mth.lerp((float)fraction,yaw,aligned);entity.pitch=Mth.lerp((float)fraction,pitch,0);
                    if(!loaded()||!RelocationSessions.clear(entity,Vec3.ZERO,1e-5)){reachable=false;break;}
                }
                if(!reachable)continue;
                entity.setPos(candidate);entity.yaw=aligned;entity.pitch=0;
                safePosition=candidate;safeYaw=aligned;
                if(place)return entity.finishProjectilePlacement();
                return true;
            }
            return false;
        }finally{if(!entity.isRemoved()){entity.setPos(before);entity.yaw=yaw;entity.pitch=pitch;entity.movingParts();}}
    }
    private void rememberLanding(){gridPose(false);}
    private void land(){
        landing=true;velocity=Vec3.ZERO;entity.setContraptionMotion(Vec3.ZERO);
        if(gridPose(true)){active=false;return;}
        if(safePosition!=null){
            Vec3 before=entity.position();float yaw=entity.yaw,pitch=entity.pitch;
            entity.setPos(safePosition);entity.yaw=safeYaw;entity.pitch=0;
            if(placementAllowed()&&entity.finishProjectilePlacement()){active=false;return;}
            entity.setPos(before);entity.yaw=yaw;entity.pitch=pitch;
        }
        // If another player occupied every safe landing cell, retain the complete entity
        // and retry. Never force a chunk load or replace their blocks/container contents.
    }
    private boolean loaded(){
        // FULL chunks around a ticking ticket may hold blocks but do not advance entities.
        // Land before crossing that boundary; otherwise the projectile can freeze in mid-air.
        if(!((ServerLevel)entity.level()).isPositionEntityTicking(entity.blockPosition()))return false;
        for(var part:entity.movingParts())for(var info:part.getContraption().getBlocks().values()){
            var box=RelocationCollision.transformed(part,info.pos(),new AABB(BlockPos.ZERO),0).bounds();
            var min=BlockPos.containing(box.minX,box.minY,box.minZ);
            var max=BlockPos.containing(box.maxX-1e-6,box.maxY-1e-6,box.maxZ-1e-6);
            if(entity.level().isOutsideBuildHeight(min)||entity.level().isOutsideBuildHeight(max)
                    ||!entity.level().hasChunksAt(min,max)||!entity.level().getWorldBorder().isWithinBounds(box))return false;
        }
        return true;
    }
    private boolean hitEntities(Vec3 motion){
        boolean collided=false;
        var level=entity.level();
        if(mass<0){mass=0;for(var part:entity.movingParts()){var world=new ContraptionWorld(level,part.getContraption());
            for(var info:part.getContraption().getBlocks().values())mass+=RelocationImpact.contribution(info.state().getDestroySpeed(world,info.pos()));}}
        for(var part:entity.movingParts()){
        var world=new ContraptionWorld(level,part.getContraption());
        for(var info:part.getContraption().getBlocks().values())for(var box:info.state().getCollisionShape(world,info.pos()).toAabbs()){
            var oriented=RelocationCollision.transformed(part,info.pos(),box,0);
            for(var target:level.getEntitiesOfClass(LivingEntity.class,oriented.bounds(),e->e.isAlive()&&!e.isSpectator()
                    &&!e.isPassengerOfSameVehicle(entity)&&!struck.contains(e.getUUID())
                    &&!(ticks<5&&e.getUUID().equals(owner)))){
                if(!RelocationCollision.overlaps(oriented,RelocationCollision.axisAligned(target.getBoundingBox())))continue;
                collided=true;
                double speed=motion.subtract(target.getDeltaMovement()).length();
                float damage=RelocationImpact.damage(mass,speed,target.getType().getCategory()==MobCategory.MONSTER);
                if(damage>0&&target.hurt(level.damageSources().thrown(entity,owner()),damage)){
                    struck.add(target.getUUID());target.push(motion.x*.6,Math.max(.1,motion.y*.4),motion.z*.6);target.hurtMarked=true;
                }
            }
        }
        }
        return collided;
    }
    void tick(){
        if(!active)return;
        if(landing){if(++ticks%10==0)land();return;}
        ++ticks;velocity=velocity.add(0,-.04,0).scale(.995);
        if(velocity.lengthSqr()>6.25)velocity=velocity.normalize().scale(2.5);
        Vec3 start=entity.position();
        var bounds=entity.getContraption().bounds;
        double radius=bounds==null?1:Math.max(bounds.getSize(),Math.max(bounds.getCenter().length(),1));
        for(var part:entity.movingParts())radius=Math.max(radius,part.position().distanceTo(entity.position())+part.getBoundingBox().getSize());
        int steps=Math.max(1,(int)Math.ceil((velocity.length()+Math.toRadians(Math.abs(spin))*radius)*16));
        for(int i=0;i<steps;i++){
            Vec3 before=entity.position();float oldYaw=entity.yaw;
            entity.yaw+=spin/steps;entity.setPos(before.add(velocity.scale(1d/steps)));
            if(!loaded()||!RelocationSessions.clear(entity,Vec3.ZERO,1e-5)){
                entity.setPos(before);entity.yaw=oldYaw;land();return;
            }
            if(hitEntities(velocity)){land();return;}
        }
        entity.setContraptionMotion(entity.position().subtract(start));
        if(ticks%4==0)rememberLanding();
    }
    void write(CompoundTag root){
        if(!active)return;
        var tag=new CompoundTag();tag.putBoolean("Landing",landing);tag.putInt("Ticks",ticks);tag.putFloat("Spin",spin);
        tag.putDouble("VX",velocity.x);tag.putDouble("VY",velocity.y);tag.putDouble("VZ",velocity.z);
        if(owner!=null)tag.putUUID("Owner",owner);tag.putString("OwnerName",ownerName);
        if(safePosition!=null){tag.putDouble("SafeX",safePosition.x);tag.putDouble("SafeY",safePosition.y);tag.putDouble("SafeZ",safePosition.z);tag.putFloat("SafeYaw",safeYaw);}
        var hits=new ListTag();for(var id:struck)hits.add(NbtUtils.createUUID(id));tag.put("Struck",hits);
        root.put("RelocationProjectile",tag);
    }
    void read(CompoundTag root){
        cancel();if(!root.contains("RelocationProjectile"))return;
        var tag=root.getCompound("RelocationProjectile");active=true;landing=tag.getBoolean("Landing");ticks=tag.getInt("Ticks");spin=tag.getFloat("Spin");
        velocity=new Vec3(tag.getDouble("VX"),tag.getDouble("VY"),tag.getDouble("VZ"));
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;ownerName=tag.getString("OwnerName");
        if(tag.contains("SafeX")){safePosition=new Vec3(tag.getDouble("SafeX"),tag.getDouble("SafeY"),tag.getDouble("SafeZ"));safeYaw=tag.getFloat("SafeYaw");}
        for(var id:tag.getList("Struck",Tag.TAG_INT_ARRAY))struck.add(NbtUtils.loadUUID(id));
        if(!Double.isFinite(velocity.lengthSqr())||!Float.isFinite(spin)){velocity=Vec3.ZERO;spin=0;landing=true;}
    }
}
