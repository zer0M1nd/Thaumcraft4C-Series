package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlanarBlocksServerTests {
 @GameTest(template="empty")
 public static void dustSlowsHorizontallyAndBoundaryRemainsSolid(GameTestHelper test){
  var pos=test.absolutePos(new BlockPos(2,2,2));var level=test.getLevel();var dust=PlanarBlocks.DUST.get().defaultBlockState();
  level.setBlockAndUpdate(pos,dust);var entity=EntityType.ARMOR_STAND.create(level);entity.setDeltaMovement(1,.5,-1);dust.entityInside(level,pos,entity);
  test.assertTrue(entity.getDeltaMovement().distanceTo(new Vec3(.4,.5,-.4))<.00001,"Dust only slows horizontal motion");
  test.assertTrue(dust.getCollisionShape(level,pos).max(Direction.Axis.Y)==.875,"Dust collision is 14/16 high");
  var boundary=PlanarBlocks.VOID.get().defaultBlockState();level.setBlockAndUpdate(pos,boundary);
  test.assertTrue(boundary.getDestroySpeed(level,pos)<0&&boundary.getLightEmission()==15&&!boundary.getCollisionShape(level,pos).isEmpty(),"Void boundary cannot be mined and remains luminous and solid");test.succeed();
 }
}
