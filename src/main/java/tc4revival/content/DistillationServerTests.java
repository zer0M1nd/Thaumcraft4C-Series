package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** Whole-chain assertions to run at the wand-system checkpoint, not during individual edits. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class DistillationServerTests {
    private DistillationServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void alumentumChangesFuelAndDistributionCadence(GameTestHelper test) {
        var origin=new BlockPos(2,1,2); test.setBlock(origin,TCArcane.ALCHEMY_FURNACE.get()); test.setBlock(origin.above(),TCArcane.ALEMBIC.get());
        var furnace=(AlchemyFurnaceBlockEntity)test.getBlockEntity(origin); var alembic=(AlembicBlockEntity)test.getBlockEntity(origin.above());
        var air=TCAspects.catalog().require("aer"); var data=new CompoundTag(); AspectNbt.write(data,new AspectAmounts().add(air,4)); furnace.load(data);
        alembic.addToContainer(air,1); furnace.setItem(0,new ItemStack(Items.DIRT)); furnace.setItem(1,new ItemStack(TCItems.ALUMENTUM.get()));
        for (int tick=0;tick<19;tick++) furnace.tick();
        test.assertTrue(alembic.amount()==1,"Accelerated distillation must still wait for its 20-tick boundary");
        furnace.tick(); test.assertTrue(alembic.amount()==2,"Alumentum supplies an existing type at tick 20 rather than tick 40");
        test.assertTrue(furnace.data.get(2)==6400 && furnace.data.get(1)==6381,"Alumentum starts a 6400-tick burn and consumes one item");
        test.assertTrue(furnace.getItem(1).isEmpty(),"Consumed alumentum must not leave a fake fuel remainder");
        // Keep fuel compatibility in this existing furnace integration batch, not per-item tests.
        for (var fuel:java.util.List.of(TCItems.BELLOWS.get(),TCItems.woodwork("greatwood_slab"))) {
            furnace.load(new CompoundTag()); furnace.setItem(0,new ItemStack(Items.DIRT));furnace.setItem(1,new ItemStack(fuel));
            furnace.tick();
            test.assertTrue(furnace.data.get(2)==300 && furnace.getItem(1).isEmpty(),"Old wooden items actually ignite the alchemy furnace for 300 ticks");
        }
        test.assertTrue(AlchemyFurnaceBlockEntity.fuelTime(new ItemStack(TCItems.GREATWOOD_LOG.get()))==300,"Vanilla wood rule wins over old unused 400-tick handler");
        test.assertTrue(AlchemyFurnaceBlockEntity.fuelTime(new ItemStack(TCItems.GREATWOOD_SAPLING.get()))==100
                && AlchemyFurnaceBlockEntity.fuelTime(new ItemStack(Items.OAK_SAPLING))==100,"Tagged saplings use native fuel duration");
        // ItemTrunkSpawner is an Item, not the separate hidden BlockMagicBox.
        var trunk=new ItemStack(TCItems.TRAVELING_TRUNK.get()); trunk.getOrCreateTag().putString("owner","fuel audit");
        furnace.load(new CompoundTag()); furnace.setItem(0,new ItemStack(Items.DIRT)); furnace.setItem(1,trunk.copy());
        test.assertTrue(AlchemyFurnaceBlockEntity.fuelTime(trunk)==0 && !furnace.canPlaceItem(1,trunk),"Traveling trunk is not fuel or accepted by the fuel slot");
        for (int tick=0;tick<40;tick++) furnace.tick();
        test.assertTrue(furnace.data.get(1)==0 && furnace.data.get(2)==0 && ItemStack.matches(trunk,furnace.getItem(1)),"A forcibly inserted portable trunk remains intact and cannot ignite");
        tc4revival.TC4Revival.LOGGER.info("PASS alumentumChangesFuelAndDistributionCadence: alumentum, wooden blocks, saplings and non-fuel traveling trunk");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void distributionPrioritizesExistingTypesThenOneEmptyAlembic(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis"); var origin=new BlockPos(2,1,2);
        test.setBlock(origin,TCArcane.ALCHEMY_FURNACE.get());
        for (int height=1;height<=3;height++) test.setBlock(origin.above(height),TCArcane.ALEMBIC.get());
        var furnace=(AlchemyFurnaceBlockEntity)test.getBlockEntity(origin);
        var first=(AlembicBlockEntity)test.getBlockEntity(origin.above());
        var second=(AlembicBlockEntity)test.getBlockEntity(origin.above(2));
        var third=(AlembicBlockEntity)test.getBlockEntity(origin.above(3));
        first.addToContainer(air,3); second.label(fire);
        var tag=new CompoundTag(); AspectNbt.write(tag,new AspectAmounts().add(air,3).add(fire,3)); furnace.load(tag);
        for (int tick=0;tick<40;tick++) furnace.tick();
        test.assertTrue(first.amount()==4 && second.amount()==1 && third.amount()==0,"Existing and pre-labelled types are filled before an eligible empty receiver");
        test.assertTrue(furnace.vis()==4,"Exactly two units must leave the furnace");
        // The first pass also filled the empty labelled receiver; the second pass excludes both transferred types.
        first.empty(); first.removeLabel(); second.empty(); second.removeLabel();
        for (int tick=0;tick<40;tick++) furnace.tick();
        test.assertTrue(first.amount()==1 && second.amount()==0 && third.amount()==0 && furnace.vis()==3,"Only one untyped receiver is seeded per transfer cycle");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void alembicTransportAndLabelsRemainSeparateFromRemoteSources(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var pos=new BlockPos(2,2,2); test.setBlock(pos,TCArcane.ALEMBIC.get());
        var alembic=(AlembicBlockEntity)test.getBlockEntity(pos); alembic.face(Direction.EAST); alembic.label(air); alembic.addToContainer(air,16);
        test.assertTrue(!(((Object)alembic) instanceof EssentiaSource),"The original alembic was not IAspectSource");
        test.assertTrue(alembic.takeEssentia(air,1,Direction.EAST)==0 && alembic.takeEssentia(air,1,Direction.DOWN)==0,"Front and bottom must not export");
        test.assertTrue(alembic.takeEssentia(air,8,Direction.UP)==8 && !alembic.acceptsPhials(),"Output pipes and empty phials may extract, but filled phials cannot pour into an alembic");
        alembic.load(alembic.saveWithoutMetadata());
        test.assertTrue(alembic.amount()==8 && alembic.facing()==Direction.EAST && alembic.filter().equals(air),"Orientation, contents and label survive save/load");
        alembic.empty(); test.assertTrue(alembic.filter().equals(air) && alembic.aspect()==null,"Emptying keeps the distinct filter"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void furnaceConsumesOneInputAndConservesDistilledAspects(GameTestHelper test) {
        var origin=new BlockPos(2,1,2); test.setBlock(origin,TCArcane.ALCHEMY_FURNACE.get()); test.setBlock(origin.above(),TCArcane.ALEMBIC.get());
        var furnace=(AlchemyFurnaceBlockEntity)test.getBlockEntity(origin); var alembic=(AlembicBlockEntity)test.getBlockEntity(origin.above());
        var input=new ItemStack(Items.DIRT); var expected=AlchemyFurnaceBlockEntity.inputAspects(input);
        test.assertTrue(!expected.isEmpty() && expected.total()>0 && expected.total()<=32,"Legacy dirt aspect fixture must be loaded");
        furnace.setItem(0,input); furnace.setItem(1,new ItemStack(Items.COAL));
        for (int tick=0;tick<5;tick++) furnace.tick();
        int burn=furnace.data.get(1),cook=furnace.data.get(0);
        var saved=furnace.saveWithoutMetadata();furnace.load(saved);
        test.assertTrue(furnace.data.get(1)==burn && furnace.data.get(0)==cook && furnace.getItem(0).getCount()==1,
                "Mid-smelt save/load preserves fuel, progress and the unconsumed input");
        for (int tick=5;tick<600;tick++) furnace.tick();
        test.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(),"One input and one fuel item must be consumed");
        var actual=furnace.aspects(); actual.add(alembic.revealedAspects());
        test.assertTrue(actual.view().equals(expected.view()),"Every input aspect must remain in the furnace or alembic without duplication or loss");
        test.assertTrue(furnace.getSlotsForFace(Direction.UP).length==0 && furnace.getSlotsForFace(Direction.DOWN)[0]==1,"Automation uses the legacy side layout");
        test.assertTrue(!furnace.canTakeItemThroughFace(1,new ItemStack(Items.COAL),Direction.DOWN) && furnace.canTakeItemThroughFace(1,new ItemStack(Items.BUCKET),Direction.DOWN),"Only empty fuel buckets leave from below"); test.succeed();
    }
}
