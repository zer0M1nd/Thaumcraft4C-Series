package automagy.gametest;
import automagy.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder("automagy")
@PrefixGameTestTemplate(false)
public final class MachineGameTests {
 private static final BlockPos POS=new BlockPos(2,2,2);
 private static <T> T place(GameTestHelper h,net.minecraft.world.level.block.Block block,Class<T> type){h.setBlock(POS,block);return type.cast(h.getLevel().getBlockEntity(h.absolutePos(POS)));}
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void thirstyTankCapacityAndSimulationUseRealCapability(GameTestHelper h){
  var tank=place(h,Content.THIRSTY_TANK.get(),ThirstyTankBlockEntity.class);
  h.assertTrue(tank.install(Direction.UP,8),"Capacity glyph rejected");var cap=tank.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("Missing fluid capability"));
  h.assertTrue(cap.fill(new FluidStack(Fluids.WATER,20000),FluidAction.SIMULATE)==20000&&tank.tank.getFluidAmount()==0,"Simulation changed tank");
  h.assertTrue(cap.fill(new FluidStack(Fluids.WATER,20000),FluidAction.EXECUTE)==20000,"Expanded tank did not accept water");
  h.assertTrue(tank.remove(Direction.UP)==-1&&tank.tank.getFluidAmount()==20000,"Removing glyph deleted excess fluid");
  cap.drain(5000,FluidAction.EXECUTE);h.assertTrue(tank.remove(Direction.UP)==8&&tank.tank.getCapacity()==16000,"Glyph could not be removed after draining");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void thirstyTankPreservesLastUnitAndReloadsGlyphs(GameTestHelper h){
  var tank=place(h,Content.THIRSTY_TANK.get(),ThirstyTankBlockEntity.class);tank.install(Direction.NORTH,6);tank.tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
  h.assertTrue(tank.tank.drain(2000,FluidAction.EXECUTE).getAmount()==999&&tank.tank.getFluidAmount()==1,"Preservation glyph lost last unit");
  var saved=tank.saveWithoutMetadata();tank.load(saved);
  h.assertTrue(tank.glyph(Direction.NORTH)==6&&tank.tank.getFluidAmount()==1&&tank.tank.drain(1,FluidAction.EXECUTE).isEmpty(),"Reload lost preservation behavior");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void experienceJarSimulationAndNbtPreserveBalance(GameTestHelper h){
  var jar=place(h,Content.EXPERIENCE_JAR.get(),ExperienceJarBlockEntity.class);
  h.assertTrue(jar.insertExperience(1000,true)==1000&&jar.experience()==0,"XP simulation mutated balance");jar.insertExperience(1000,false);
  h.assertTrue(jar.extractExperience(300,true)==300&&jar.experience()==1000,"XP drain simulation mutated balance");jar.extractExperience(300,false);
  var saved=jar.saveWithoutMetadata();jar.load(saved);h.assertTrue(jar.experience()==700&&jar.asItem().getTagElement("BlockEntityTag").getInt("XP")==700,"XP save/item output lost balance");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void workbenchProtectsTemplateMaterialsButExposesOutputs(GameTestHelper h){
  var bench=place(h,Content.GOLEM_WORKBENCH.get(),GolemWorkbenchBlockEntity.class);
  bench.template(0,0,new ItemStack(Items.OAK_LOG));bench.materials().setStackInSlot(0,new ItemStack(Items.OAK_LOG,3));bench.materials().setStackInSlot(1,new ItemStack(Items.OAK_PLANKS,4));
  var cap=bench.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("Missing item capability"));
  h.assertTrue(cap.extractItem(0,3,false).isEmpty()&&bench.materials().getStackInSlot(0).getCount()==3,"Automation extracted reserved material");
  h.assertTrue(cap.extractItem(1,4,false).getCount()==4&&bench.materials().getStackInSlot(1).isEmpty(),"Automation could not extract output");
  var saved=bench.saveWithoutMetadata();bench.load(saved);h.assertTrue(bench.template(0,0).is(Items.OAK_LOG)&&bench.materials().getStackInSlot(0).getCount()==3,"Workbench reload lost template/material");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void workbenchRequiresExplicitMatchingGolemOutputFilter(GameTestHelper h){
  var golem=tc4revival.content.TCEntities.GOLEM.get().create(h.getLevel());
  var output=new ItemStack(Items.OAK_PLANKS,4);
  h.assertTrue(!GolemWorkbenchBlockEntity.acceptsOutput(golem,output),"Empty filter allowed workbench production");
  golem.filters().sample(0,new ItemStack(Items.OAK_LOG));
  h.assertTrue(!GolemWorkbenchBlockEntity.acceptsOutput(golem,output),"Ingredient filter matched the output");
  golem.filters().sample(0,new ItemStack(Items.OAK_PLANKS));
  h.assertTrue(GolemWorkbenchBlockEntity.acceptsOutput(golem,output),"Matching output sample rejected a larger output stack");
  var named=output.copy();named.setHoverName(net.minecraft.network.chat.Component.literal("Different NBT"));
  h.assertTrue(!GolemWorkbenchBlockEntity.acceptsOutput(golem,named),"Output NBT was ignored without the toggle");
  golem.filters().sample(0,ItemStack.EMPTY);
  h.assertTrue(!GolemWorkbenchBlockEntity.acceptsOutput(golem,output),"Removing the filter did not revoke production");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void aggregatorCollisionMatchesBothHalves(GameTestHelper h){
  var block=Content.LOCUS_AGGREGATOR.get();var world=h.getLevel();var pos=h.absolutePos(POS);
  var lower=block.defaultBlockState();h.setBlock(POS,lower);var a=lower.getCollisionShape(world,pos);
  var upper=lower.setValue(LocusAggregatorBlock.HALF,net.minecraft.world.level.block.state.properties.Half.TOP);h.setBlock(POS,upper);var b=upper.getCollisionShape(world,pos);
  h.assertTrue(a.min(Direction.Axis.Y)==0&&a.max(Direction.Axis.Y)==.5&&b.min(Direction.Axis.Y)==.5&&b.max(Direction.Axis.Y)==1,"Aggregator uses a full block collision");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void dispensersLaunchBothPearlsAndRememberReturnOrigin(GameTestHelper h){
  var block=net.minecraft.world.level.block.Blocks.DISPENSER;var state=block.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING,Direction.EAST);h.setBlock(POS,state);
  var pos=h.absolutePos(POS);var level=h.getLevel();var tile=(net.minecraft.world.level.block.entity.DispenserBlockEntity)level.getBlockEntity(pos);
  for(var item:java.util.List.of(Content.AVARICE_PEARL.get(),Content.MISERS_PEARL.get())){
   tile.setItem(0,new ItemStack(item,2));block.tick(state,level,pos,level.random);
   var pearls=level.getEntitiesOfClass(AvaricePearlEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
   h.assertTrue(pearls.size()==1&&pearls.get(0).getItem().is(item),"Dispenser dropped item instead of correct projectile");
   h.assertTrue(tile.getItem(0).getCount()==1,"Dispenser consumed incorrect pearl count");
   var data=new net.minecraft.nbt.CompoundTag();pearls.get(0).addAdditionalSaveData(data);
   h.assertTrue(data.contains("OriginY")&&Math.abs(data.getDouble("OriginX")-pearls.get(0).getX())<.001,"Dispenser return origin was not persisted");pearls.get(0).discard();
  }h.succeed();
 }
}
