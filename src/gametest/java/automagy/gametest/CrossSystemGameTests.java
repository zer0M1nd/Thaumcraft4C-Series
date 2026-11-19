package automagy.gametest;

import automagy.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("automagy")
@PrefixGameTestTemplate(false)
public final class CrossSystemGameTests {
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void boilerRetainsAdjacentFacesAndDoesNotBlockItsOwnLight(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,Content.BOILER.get());
  var level=h.getLevel();var absolute=h.absolutePos(pos);var state=level.getBlockState(absolute);
  h.assertTrue(state.getLightBlock(level,absolute)==0,"Boiler blocks the light sampled by its custom model");
  h.assertTrue(state.propagatesSkylightDown(level,absolute),"Boiler excludes skylight sources despite zero light blocking");
  h.assertTrue(state.getOcclusionShape(level,absolute).isEmpty(),"Boiler must not present a full cube to the skylight source heightmap");
  h.assertTrue(net.minecraft.world.level.block.Block.shouldRenderFace(Blocks.STONE.defaultBlockState(),level,absolute.below(),Direction.UP,absolute),"Boiler culls the floor below its rounded model");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void redcrystalNetworkDrainsAfterExternalPowerRemoved(GameTestHelper h){
  var a=new BlockPos(2,2,2);var b=a.east();var source=a.west();
  h.setBlock(a.below(),Blocks.STONE);h.setBlock(b.below(),Blocks.STONE);
  h.setBlock(a,Content.REDCRYSTALS.get("standard").get());h.setBlock(b,Content.REDCRYSTALS.get("standard").get());h.setBlock(source,Blocks.REDSTONE_BLOCK);
  var level=h.getLevel();var first=(RedcrystalBlockEntity)level.getBlockEntity(h.absolutePos(a));var second=(RedcrystalBlockEntity)level.getBlockEntity(h.absolutePos(b));
  RedcrystalNetwork.update(level,h.absolutePos(a));
  h.assertTrue(first.power==15&&second.power==14,"Two-crystal network did not carry attenuated vanilla power");
  h.setBlock(source,Blocks.AIR);RedcrystalNetwork.update(level,h.absolutePos(a));
  h.assertTrue(first.power==0&&second.power==0,"Crystals fed their stale output back as an external source");
  h.assertTrue(level.getBlockState(h.absolutePos(b)).getValue(RedcrystalBlock.POWER)==0,"Blockstate retained powered display");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void milkBoilerTransfersAllThreeAspectsToBaseAlembics(GameTestHelper h){
  var pos=new BlockPos(3,1,3);h.setBlock(pos,Content.BOILER.get());
  var level=h.getLevel();var absolute=h.absolutePos(pos);var boiler=(BoilerBlockEntity)level.getBlockEntity(absolute);
  for(int y=1;y<=3;y++)h.setBlock(pos.above(y),TCArcane.ALEMBIC.get());
  boiler.tank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.common.ForgeMod.MILK.get(),1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  boiler.fuelInventory().setStackInSlot(0,new ItemStack(TCItems.ALUMENTUM.get()));
  for(int i=0;i<240;i++)BoilerBlockEntity.tick(level,absolute,level.getBlockState(absolute),boiler);
  var amounts=new java.util.HashMap<String,Integer>();
  for(int y=1;y<=3;y++){var alembic=(AlembicBlockEntity)level.getBlockEntity(absolute.above(y));if(alembic.aspect()!=null)amounts.merge(alembic.aspect().tag(),alembic.amount(),Integer::sum);}
  h.assertTrue(amounts.equals(java.util.Map.of("aqua",2,"fames",2,"sano",2)),"Milk batch was lost, duplicated, or contained bucket aspects: "+amounts);
  h.assertTrue(boiler.tank.isEmpty()&&boiler.storedEssentia()==0&&boiler.fuelInventory().getStackInSlot(0).isEmpty(),"Boiler did not consume one batch and one fuel");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void infinityJarTransportEnforcesFaceAndAspectAfterReload(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos,Content.INFINITY_JAR.get());
  var jar=(InfinityJarBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));var aspects=tc4revival.aspect.TCAspects.catalog();var fire=aspects.require("ignis");var water=aspects.require("aqua");
  jar.select(fire);var transport=(thaumcraft.api.aspects.IEssentiaTransport)jar;
  h.assertTrue(transport.takeEssentia(fire,1,Direction.DOWN)==0,"Jar exported from closed face");
  h.assertTrue(transport.takeEssentia(water,1,Direction.UP)==0,"Jar exported wrong aspect");
  h.assertTrue(transport.takeEssentia(fire,1,Direction.UP)==1&&jar.amount()==1,"Infinity jar lost protected last unit");
  var saved=jar.saveWithoutMetadata();jar.load(saved);
  h.assertTrue(transport.takeEssentia(fire,1,Direction.UP)==1&&jar.aspect().equals(fire),"Reload broke public essentia transport");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void beaconMixinAcceptsRunesOnlyInFirstLayer(GameTestHelper h) throws Exception {
  var center=new BlockPos(3,3,3);
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(center.offset(x,-1,z),Content.RUNED_OBSIDIAN.get());
  var method=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(net.minecraft.world.level.block.entity.BeaconBlockEntity.class,"m_155092_",net.minecraft.world.level.Level.class,int.class,int.class,int.class);
  var p=h.absolutePos(center);
  h.assertTrue((int)method.invoke(null,h.getLevel(),p.getX(),p.getY(),p.getZ())==1,"Actual transformed vanilla beacon rejected runic first layer");
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.setBlock(center.offset(x,-2,z),Blocks.IRON_BLOCK);
  h.assertTrue((int)method.invoke(null,h.getLevel(),p.getX(),p.getY(),p.getZ())==2,"Vanilla second layer below runes rejected");
  h.setBlock(center.offset(0,-2,0),Content.RUNED_OBSIDIAN.get());
  h.assertTrue((int)method.invoke(null,h.getLevel(),p.getX(),p.getY(),p.getZ())==1,"Rune incorrectly counted in deeper layer");h.succeed();
 }
}
