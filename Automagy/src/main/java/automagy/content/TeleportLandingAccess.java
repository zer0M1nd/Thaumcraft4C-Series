package automagy.content;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
/** Read-only landing validation. Never moves the entity or loads an adjacent chunk. */
public final class TeleportLandingAccess {
 public static OptionalDouble find(LivingEntity entity,Vec3 requested){
  var level=entity.level();
  if(!Double.isFinite(requested.x)||!Double.isFinite(requested.y)||!Double.isFinite(requested.z)||Math.abs(requested.x)>30000000||Math.abs(requested.z)>30000000)return OptionalDouble.empty();
  var target=BlockPos.containing(requested);if(level.isOutsideBuildHeight(target)||!level.hasChunkAt(target))return OptionalDouble.empty();
  return automagy.logic.TeleportLanding.find(requested.y,level.getMinBuildHeight(),y->level.getBlockState(new BlockPos(target.getX(),y,target.getZ())).blocksMotion(),y->{
   var box=entity.getBoundingBox().move(requested.x-entity.getX(),y-entity.getY(),requested.z-entity.getZ());
   for(int x=(int)Math.floor(box.minX)>>4;x<=((int)Math.floor(box.maxX)>>4);x++)for(int z=(int)Math.floor(box.minZ)>>4;z<=((int)Math.floor(box.maxZ)>>4);z++)if(!level.hasChunk(x,z))return false;
   return level.noCollision(entity,box)&&!level.containsAnyLiquid(box);
  });
 }
 private TeleportLandingAccess(){}
}
