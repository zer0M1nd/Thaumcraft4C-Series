package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import thmodern.Horizons;

public final class PlaneTravel {
 private static final String COOLDOWN="thaumichorizons:plane_cooldown";
 private PlaneTravel(){}
 public static void complete(ServerLevel level,PocketPlanes data,PocketPlanes.Plane plane){
  if(!plane.stage().equals("arrival_portal")||!level.dimension().location().equals(PocketPlanes.DIMENSION))throw new IllegalArgumentException("Plane is not at portal stage");
  var origin=level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,plane.origin().dimension()));if(origin==null)throw new IllegalStateException("Source dimension unavailable");origin.getChunkAt(plane.origin().pos());
  if(!(origin.getBlockEntity(plane.origin().pos()) instanceof VortexEntity source)||source.planeId()!=plane.id()||!plane.allocation().equals(source.allocation())||!source.hasEscrow())throw new IllegalStateException("Source rift/pearl escrow no longer matches generation request");
  var center=plane.center();var stone=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:arcane_stone"));
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){level.setBlockAndUpdate(center.offset(x,0,z),stone.defaultBlockState());level.setBlockAndUpdate(center.offset(x,1,z),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(center.offset(x,2,z),Blocks.AIR.defaultBlockState());}
  level.setBlockAndUpdate(center.above(),Horizons.VORTEX.get().defaultBlockState());if(!(level.getBlockEntity(center.above()) instanceof VortexEntity arrival))throw new IllegalStateException("Arrival rift creation failed");
  arrival.initialize(source.aspects(),true);arrival.bindPlane(plane,true);data.progress(plane.id(),"ready",0,true);source.commitPearl();
 }
 public static void touch(VortexEntity vortex,PocketPlanes.Plane plane,ServerLevel level){
  if(!plane.ready()||!plane.allocation().equals(vortex.allocation()))return;
  for(var player:level.getEntitiesOfClass(ServerPlayer.class,new AABB(vortex.getBlockPos())))if(!player.isPassenger()&&!player.isVehicle()&&!player.isSpectator()){
   long now=level.getServer().overworld().getGameTime();var saved=player.getPersistentData();if(saved.getLong(COOLDOWN)>now){saved.putLong(COOLDOWN,now+10);continue;}
   if(transfer(player,plane)){saved.putLong(COOLDOWN,now+200);}
  }
 }
 public static boolean transfer(ServerPlayer player,PocketPlanes.Plane plane){
  if(!plane.ready())return false;boolean leaving=player.level().dimension().location().equals(PocketPlanes.DIMENSION);
  var destination=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,leaving?plane.origin().dimension():PocketPlanes.DIMENSION));
  if(!leaving&&!thmodern.LegacyConfig.pocket())return false;
  BlockPos desired=leaving?plane.origin().pos():plane.center().above();
  if(!leaving){if(destination==null)return false;destination.getChunkAt(desired);if(!(destination.getBlockEntity(desired) instanceof VortexEntity arrival)||!plane.allocation().equals(arrival.allocation()))return false;}
  Optional<Vec3> safe=destination==null?Optional.empty():safePosition(destination,desired,player);
  if(safe.isEmpty()&&leaving){destination=player.server.overworld();safe=safePosition(destination,destination.getSharedSpawnPos(),player);}
  if(destination==null||safe.isEmpty())return false;var at=safe.get();player.closeContainer();player.teleportTo(destination,at.x,at.y,at.z,player.getYRot(),player.getXRot());player.fallDistance=0;player.setDeltaMovement(Vec3.ZERO);return true;
 }
 static Optional<Vec3> safePosition(ServerLevel level,BlockPos center,ServerPlayer player){
  level.getChunkAt(center);for(int radius=0;radius<=8;radius++)for(int dy=0;dy<=8;dy++)for(int sign:new int[]{1,-1})for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
   if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=center.offset(x,dy*sign,z);if(level.isOutsideBuildHeight(at)||level.isOutsideBuildHeight(at.above())||!level.getWorldBorder().isWithinBounds(at))continue;
   if(!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)||!level.getFluidState(at).isEmpty()||!level.getFluidState(at.above()).isEmpty())continue;
   var location=new Vec3(at.getX()+.5,at.getY(),at.getZ()+.5);var shape=player.getDimensions(player.getPose()).makeBoundingBox(location);if(level.noCollision(player,shape))return Optional.of(location);
  }return Optional.empty();
 }
}
