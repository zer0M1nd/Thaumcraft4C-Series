package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlanarDecorationServerTests {
 @GameTest(template="empty")
 public static void spikeFacesRequireSupportAndHurt(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,4,2));
  for(var facing:Direction.values()){
   var support=pos.relative(facing.getOpposite());level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
   var block=PlanarDecorations.SPIKES.get("metal").get();var state=block.defaultBlockState().setValue(SpikeBlock.FACING,facing);test.assertTrue(state.canSurvive(level,pos),"Support works on "+facing);level.setBlockAndUpdate(pos,state);
   var box=state.getCollisionShape(level,pos,CollisionContext.empty()).bounds();test.assertTrue(Math.abs((box.max(facing.getAxis())-box.min(facing.getAxis()))-15/16d)<.001,"One-sixteenth tip gap "+facing);
   level.removeBlock(support,false);test.assertTrue(level.isEmptyBlock(pos),"Lost support removes the spike "+facing);
  }
  var pig=EntityType.PIG.create(level);pig.moveTo(pos.getCenter());float before=pig.getHealth();var state=PlanarDecorations.SPIKES.get("tooth").get().defaultBlockState();state.getBlock().entityInside(state,level,pos,pig);test.assertTrue(pig.getHealth()==before-1,"Tooth spike deals one cactus damage");pig.discard();test.succeed();
 }
 @GameTest(template="empty")
 public static void crystalsKeepColorsLightAndDrops(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));
  for(var color:DyeColor.values()){
   var block=PlanarDecorations.CRYSTALS.get(color.getName()).get();var state=block.defaultBlockState();test.assertTrue(state.getLightBlock(level,pos)==1,"Colored crystal filters one light level");
   var drops=Block.getDrops(state,level,pos,null);test.assertTrue(drops.size()==1&&drops.get(0).is(block.asItem()),"Crystal drops its own color "+color);
  }
  test.assertTrue(PlanarDecorations.DEEP_CRYSTAL.get().defaultBlockState().getLightEmission()==15,"Deep crystal emits full light");test.succeed();
 }
}
