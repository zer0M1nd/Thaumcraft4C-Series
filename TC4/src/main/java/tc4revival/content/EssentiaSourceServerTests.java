package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.TCAspects;

/** Source discovery, cache behavior and real inventory changes in one server world. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class EssentiaSourceServerTests {
    private EssentiaSourceServerTests() {}
    @GameTest(template="tree_test_empty") public static void infusionAvailabilityCountsRemoteAndLocalWithoutConsumptionOrDuplication(GameTestHelper test){
        var world=test.getLevel();var origin=new BlockPos(17,8,17);test.setBlock(origin,TCArcane.ESSENTIA_RESERVOIR.get());var consumer=test.getBlockEntity(origin);
        var air=TCAspects.catalog().require("aer");var wanted=new tc4revival.aspect.AspectAmounts().add(air,100);
        var local=jar(test,origin.east(2),12);
        var multiPos=origin.west(2);test.setBlock(multiPos,TCArcane.ESSENTIA_RESERVOIR.get());var multi=(EssentiaReservoirBlockEntity)test.getBlockEntity(multiPos);multi.addToContainer(air,30);
        var near=mirror(test,origin.north(2),Direction.EAST);var far=mirror(test,origin.offset(22,0,0),Direction.EAST);near.remember(net.minecraft.core.GlobalPos.of(world.dimension(),far.getBlockPos()));test.assertTrue(near.restoreLink(),"Mirror pair links");
        var remote=jar(test,origin.offset(24,0,0),20);
        test.assertTrue(EssentiaSources.available(consumer,wanted).amount(air)==62,"Remote mirror and local non-jar storage contribute to availability");
        var proxyPos=test.absolutePos(origin.south(2));world.setBlock(proxyPos,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),2);world.removeBlockEntity(proxyPos);
        world.setBlockEntity(new EssentiaJarBlockEntity(proxyPos,TCArcane.ESSENTIA_JAR.get().defaultBlockState()){
            @Override public java.util.List<? extends EssentiaSource> availabilitySources(){return java.util.List.of(local,multi,remote,this);}
            @Override public int availableAmount(tc4revival.aspect.Aspect aspect){return 0;}
        });
        var result=EssentiaSources.available(consumer,wanted);
        test.assertTrue(result.amount(air)==62,"Jars, shared reservoir and remote mirror are counted exactly once through overlapping proxies");
        test.assertTrue(local.amount()==12&&remote.amount()==20&&multi.contains(air,30),"HUD never consumes stored essentia");test.succeed();
    }

    @GameTest(template="tree_test_empty")
    public static void mirrorCycleStopsInsteadOfRecursivelyOverflowing(GameTestHelper test) {
        var a=mirror(test,new BlockPos(5,8,5),Direction.EAST);
        var b=mirror(test,new BlockPos(25,8,25),Direction.NORTH);
        var c=mirror(test,new BlockPos(25,8,24),Direction.NORTH);
        var d=mirror(test,new BlockPos(5,8,4),Direction.SOUTH);
        // Direct state placement avoids support updates while constructing this artificial cycle.
        a.remember(net.minecraft.core.GlobalPos.of(test.getLevel().dimension(),b.getBlockPos()));
        c.remember(net.minecraft.core.GlobalPos.of(test.getLevel().dimension(),d.getBlockPos()));
        test.assertTrue(a.restoreLink() && c.restoreLink(),"Both pairs are reciprocal before searching");
        test.assertTrue(!a.takeFromContainer(TCAspects.catalog().require("aer"),1),
                "First-cache mirror traversal terminates even when the remote ranges form a cycle");
        test.assertTrue(a.valid(false) && c.valid(false),"Loop rejection does not destroy valid pairings");
        test.succeed();
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void mirrorDrainsTheTargetDimensionWithoutTouchingALocalJar(GameTestHelper test) {
        var local=test.getLevel(); var remote=local.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        test.assertTrue(remote!=null,"The cross-dimension fixture requires the vanilla Nether");
        var a=mirror(test,new BlockPos(17,8,17),Direction.EAST);
        var remotePos=new BlockPos(a.getBlockPos().getX(),96,a.getBlockPos().getZ()); var sourcePos=remotePos.east(2);
        remote.getChunk(remotePos); remote.getChunk(sourcePos);
        var beforeMirror=net.minecraftforge.common.util.BlockSnapshot.create(remote.dimension(),remote,remotePos);
        var beforeSource=net.minecraftforge.common.util.BlockSnapshot.create(remote.dimension(),remote,sourcePos);
        var localJar=jar(test,new BlockPos(19,8,17),4); var air=TCAspects.catalog().require("aer");
        try {
            remote.setBlock(remotePos,TCArcane.ESSENTIA_MIRROR.get().defaultBlockState().setValue(EssentiaMirrorBlock.FACING,Direction.EAST),2);
            remote.setBlock(sourcePos,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),2);
            var remoteJar=(EssentiaJarBlockEntity)remote.getBlockEntity(sourcePos); remoteJar.addToContainer(air,4);
            a.remember(net.minecraft.core.GlobalPos.of(remote.dimension(),remotePos));
            test.assertTrue(a.restoreLink() && a.takeFromContainer(air,1),"A real mirror pair can drain across dimensions");
            test.assertTrue(remoteJar.amount()==3 && localJar.amount()==4,"Only the remote dimension pays the unit");
            var tag=a.saveWithoutMetadata(); tag.remove("linkY");
            var restored=new EssentiaMirrorBlockEntity(a.getBlockPos(),a.getBlockState()); restored.load(tag);
            test.assertTrue(!restored.linked() && restored.mirrorDestination()==null,"Incomplete saved coordinates cannot produce an unintended origin link");
        } finally {
            beforeMirror.restore(true,false); beforeSource.restore(true,false);
        }
        test.succeed();
    }
    private static EssentiaMirrorBlockEntity mirror(GameTestHelper test,BlockPos pos,Direction facing) {
        var absolute=test.absolutePos(pos);
        test.getLevel().setBlock(absolute,TCArcane.ESSENTIA_MIRROR.get().defaultBlockState().setValue(EssentiaMirrorBlock.FACING,facing),2);
        return (EssentiaMirrorBlockEntity)test.getLevel().getBlockEntity(absolute);
    }
    @GameTest(template="tree_test_empty")
    public static void mirrorVariantIdentityDoesNotInventDestinationCoordinates(GameTestHelper test) {
        var plain=new net.minecraft.world.item.ItemStack(TCItems.ESSENTIA_MIRROR.get());
        var plainKey=tc4revival.aspect.item.ItemStackAspects.identity(plain).orElseThrow();
        test.assertTrue(plainKey.item().equals("Thaumcraft:blockMirror") && plainKey.metadata()==6,"A new mirror has the old unpaired identity");
        var target=net.minecraft.core.GlobalPos.of(test.getLevel().dimension(),test.absolutePos(new BlockPos(5,8,17)));
        var linked=EssentiaMirrorItem.linkedStack(target,"Overworld");
        test.assertTrue(tc4revival.aspect.item.ItemStackAspects.identity(linked).orElseThrow().metadata()==7
                        && target.equals(EssentiaMirrorItem.destination(linked)),"A prepared mirror has both paired identity and real target data");
        var display=new net.minecraft.world.item.ItemStack(TCItems.ESSENTIA_MIRROR.get()); display.getOrCreateTag().putInt("tc4_metadata",7);
        test.assertTrue(EssentiaMirrorItem.legacyMetadata(display)==7 && EssentiaMirrorItem.destination(display)==null,
                "A paired book-display variant without coordinates cannot create an origin link");
        var broken=linked.copy(); broken.getTag().remove("linkZ");
        test.assertTrue(EssentiaMirrorItem.destination(broken)==null,"Incomplete destination coordinates are rejected instead of defaulting to zero");
        test.succeed();
    }
    @GameTest(template="tree_test_empty")
    public static void mirrorPairDrainsAcrossItsPartnerAndRecoversFromBrokenReciprocity(GameTestHelper test) {
        var left=new BlockPos(5,8,17); var right=new BlockPos(25,8,17); var world=test.getLevel();
        test.setBlock(left.west(),Blocks.STONE); test.setBlock(right.west(),Blocks.STONE);
        test.setBlock(left,TCArcane.ESSENTIA_MIRROR.get().defaultBlockState().setValue(EssentiaMirrorBlock.FACING,Direction.EAST));
        test.setBlock(right,TCArcane.ESSENTIA_MIRROR.get().defaultBlockState().setValue(EssentiaMirrorBlock.FACING,Direction.EAST));
        var a=(EssentiaMirrorBlockEntity)test.getBlockEntity(left); var b=(EssentiaMirrorBlockEntity)test.getBlockEntity(right);
        a.remember(net.minecraft.core.GlobalPos.of(world.dimension(),test.absolutePos(right)));
        test.assertTrue(a.restoreLink() && a.valid(false) && b.valid(false),"Restoring establishes reciprocal coordinates on both mirrors");
        var jar=jar(test,right.east(2),3); var air=TCAspects.catalog().require("aer");
        test.assertTrue(!a.contains(air,1) && !a.takeFromContainer(air,2),"Mirrors do not advertise availability or allow multi-unit drains");
        test.assertTrue(a.takeFromContainer(air,1) && jar.amount()==2,"The first mirror drains a jar in front of its distant partner");
        var drops=net.minecraft.world.level.block.Block.getDrops(a.getBlockState(),world,a.getBlockPos(),a);
        test.assertTrue(drops.size()==1 && drops.get(0).is(TCItems.ESSENTIA_MIRROR.get())
                        && a.mirrorDestination().equals(EssentiaMirrorItem.destination(drops.get(0))),
                "A paired mirror's dropped item retains its target");
        test.assertTrue(a.valid(false) && b.valid(false),"Inspecting drops must not disconnect the live pair");
        var saved=a.saveWithoutMetadata(); var restored=new EssentiaMirrorBlockEntity(a.getBlockPos(),a.getBlockState()); restored.load(saved);
        test.assertTrue(restored.linked() && restored.mirrorDestination().equals(a.mirrorDestination()),"Modern dimension keys and link coordinates survive saving");
        b.remember(net.minecraft.core.GlobalPos.of(world.dimension(),test.absolutePos(right.above(2))));
        test.assertTrue(!a.valid(true) && !a.linked(),"A partner that no longer points back invalidates the link");
        test.assertTrue(a.restoreLink() && b.valid(false),"An unpaired partner can reconnect to the retained destination");
        test.setBlock(left,Blocks.AIR);
        test.assertTrue(!b.linked(),"Actually removing the mirror disconnects its partner");
        test.setBlock(left,TCArcane.ESSENTIA_MIRROR.get().defaultBlockState().setValue(EssentiaMirrorBlock.FACING,Direction.EAST));
        var replacement=(EssentiaMirrorBlockEntity)test.getBlockEntity(left);
        replacement.remember(EssentiaMirrorItem.destination(drops.get(0)));
        test.assertTrue(replacement.restoreLink() && b.valid(false),"The carried target reconnects a replacement mirror");
        test.succeed();
    }
    @GameTest(template="tree_test_empty")
    public static void reservoirRecipeDropAndFiveTickIntakeConnect(GameTestHelper test) {
        var world=test.getLevel(); var pos=new BlockPos(17,8,17); var air=TCAspects.catalog().require("aer");
        test.setBlock(pos,TCArcane.ESSENTIA_RESERVOIR.get()); test.setBlock(pos.below(),TCArcane.ALEMBIC.get());
        var reservoir=(EssentiaReservoirBlockEntity)test.getBlockEntity(pos);
        var alembic=(AlembicBlockEntity)test.getBlockEntity(pos.below()); alembic.addToContainer(air,10);
        for (int tick=0;tick<4;tick++) reservoir.tick();
        test.assertTrue(reservoir.amount()==0 && alembic.amount()==10,"Reservoir intake waits for its fifth tick");
        reservoir.tick();
        test.assertTrue(reservoir.contains(air,1) && alembic.amount()==9,"The downward port draws exactly one unit from the lower alembic");
        reservoir.face(Direction.UP);
        for (int tick=0;tick<5;tick++) reservoir.tick();
        test.assertTrue(reservoir.amount()==1 && alembic.amount()==9,"Rotating away disconnects that intake");
        var drops=net.minecraft.world.level.block.Block.getDrops(reservoir.getBlockState(),world,test.absolutePos(pos),reservoir);
        test.assertTrue(drops.size()==1 && drops.get(0).is(TCItems.ESSENTIA_RESERVOIR.get()) && !drops.get(0).hasTag(),
                "The reservoir drops its empty item, not stored essentia");
        var recipe=(InfusionRecipe)world.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/essentia_reservoir")).orElseThrow();
        var ingredients=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for (int index=0;index<6;index++) ingredients.add(new net.minecraft.world.item.ItemStack(index%3==0 ? TCItems.material(MaterialType.VOID) : TCItems.ESSENTIA_JAR.get()));
        var input=new InfusionRecipe.Input(new net.minecraft.world.item.ItemStack(TCItems.ESSENTIA_BUFFER.get()),ingredients,key->key.equals("ESSENTIARESERVOIR"));
        test.assertTrue(recipe.matches(input,world) && recipe.assemble(input,world.registryAccess()).is(TCItems.ESSENTIA_RESERVOIR.get()),
                "The researched buffer, two void ingots and four jars produce a reservoir");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(input.getItem(0),ingredients,key->false),world),
                "The acquisition route still requires its research");
        reservoir.takeFromContainer(air,1); test.succeed();
    }
    @GameTest(template="tree_test_empty")
    public static void reservoirSharesCapacityAndServesRemoteConsumers(GameTestHelper test) {
        var pos=new BlockPos(17,8,17); var air=TCAspects.catalog().require("aer"); var water=TCAspects.catalog().require("aqua");
        test.setBlock(pos,TCArcane.ESSENTIA_RESERVOIR.get());
        var reservoir=(EssentiaReservoirBlockEntity)test.getBlockEntity(pos);
        test.assertTrue(reservoir.addToContainer(air,200)==0 && reservoir.addToContainer(water,100)==44,
                "Different aspects share a single 256-unit capacity");
        test.assertTrue(reservoir.suction(Direction.DOWN)==0 && reservoir.minimumSuction()==24,"A full reservoir stops pulling");
        test.assertTrue(reservoir.essentiaType(Direction.DOWN)==null && reservoir.essentiaType(null)==air,
                "Only UNKNOWN inspection advertises the first stored aspect");
        var state=test.getLevel().getBlockState(test.absolutePos(pos));
        test.assertTrue(state.getAnalogOutputSignal(test.getLevel(),test.absolutePos(pos))==15,"A full reservoir gives maximum comparator strength");
        var restored=new EssentiaReservoirBlockEntity(test.absolutePos(pos),state); restored.load(reservoir.saveWithoutMetadata());
        test.assertTrue(restored.contains(air,200) && restored.contains(water,56) && restored.facing()==Direction.DOWN,"Save/load preserves mixed contents and the port");
        test.setBlock(pos.east(2),TCArcane.INFUSION_MATRIX.get());
        var matrix=(InfusionMatrixBlockEntity)test.getBlockEntity(pos.east(2));
        test.assertTrue(matrix.drainEssentia(water) && reservoir.contains(water,55) && reservoir.amount()==255,
                "An actual matrix discovers a reservoir and drains the requested aspect");
        test.assertTrue(reservoir.suction(Direction.DOWN)==24,"One unit of vacancy immediately restores suction");
        reservoir.face(Direction.EAST);
        test.assertTrue(reservoir.takeEssentia(air,1,Direction.DOWN)==0 && reservoir.takeEssentia(air,1,Direction.EAST)==1,
                "Rotating the port changes both input and output access");
        var corrupted=reservoir.saveWithoutMetadata();
        tc4revival.aspect.AspectNbt.write(corrupted,new tc4revival.aspect.AspectAmounts().add(air,257)); restored.load(corrupted);
        test.assertTrue(restored.amount()==257&&restored.addToContainer(air,1)==1,"Over-capacity saved contents remain extractable and reject further insertion");
        // Avoid leaving a charged block for the test structure's later teardown.
        for (var aspect:reservoir.revealedAspects().aspects()) reservoir.takeFromContainer(aspect,reservoir.revealedAspects().amount(aspect));
        test.succeed();
    }
    @GameTest(template="tree_test_empty")
    public static void directionalSearchKeepsItsOpenForwardBoundary(GameTestHelper test) {
        var origin=new BlockPos(17,8,17); var air=TCAspects.catalog().require("aer");
        test.setBlock(origin,TCArcane.INFUSION_MATRIX.get());
        var matrix=test.getBlockEntity(origin);
        var excluded=jar(test,origin.east(3),3);
        var behind=jar(test,origin.west(),3);
        var included=jar(test,origin.east(2),3);
        test.assertTrue(EssentiaSources.contains(matrix,air,Direction.EAST,3),"The east search finds the source two blocks forward");
        test.assertTrue(included.amount()==3 && excluded.amount()==3 && behind.amount()==3,"Looking up a source consumes nothing");
        test.assertTrue(EssentiaSources.drain(matrix,air,Direction.EAST,3,false).orElseThrow().equals(test.absolutePos(origin.east(2))),
                "Only the source strictly inside the forward range is eligible");
        test.assertTrue(included.amount()==2 && excluded.amount()==3 && behind.amount()==3,"A drain removes exactly one eligible unit");
        included.takeFromContainer(air,2);
        test.assertTrue(EssentiaSources.drain(matrix,air,Direction.EAST,3,false).orElseThrow().equals(excluded.getBlockPos())&&behind.amount()==3,
                "The forward endpoint is included while the source behind remains excluded");
        test.succeed();
    }
    @GameTest(template="tree_test_empty")
    public static void missingCachedSourceStopsSearchAndRefreshPreservesRetryDelay(GameTestHelper test) {
        var origin=new BlockPos(17,8,17); var air=TCAspects.catalog().require("aer");
        test.setBlock(origin,TCArcane.INFUSION_MATRIX.get()); var matrix=test.getBlockEntity(origin);
        var first=jar(test,origin.west(),3); var later=jar(test,origin.east(),3);
        test.assertTrue(EssentiaSources.drain(matrix,air,null,2,false).orElseThrow().equals(first.getBlockPos()),
                "Undirected traversal visits the negative x source first");
        test.setBlock(origin.west(),Blocks.AIR);
        test.assertTrue(EssentiaSources.drain(matrix,air,null,2,false).isPresent() && later.amount()==2,
                "A missing cached tile does not block another valid source");
        EssentiaSources.refresh(matrix);
        test.assertTrue(EssentiaSources.drain(matrix,air,null,2,false).isPresent()&&later.amount()==1,
                "Explicit refresh makes valid sources immediately available");
        test.succeed();
    }
    @GameTest(template="tree_test_empty")
    public static void matrixUsesJarsButLeavesAlembicsForPipeTransport(GameTestHelper test) {
        var origin=new BlockPos(17,8,17); var air=TCAspects.catalog().require("aer");
        test.setBlock(origin,TCArcane.INFUSION_MATRIX.get());
        test.setBlock(origin.west(),TCArcane.ALEMBIC.get());
        var alembic=(AlembicBlockEntity)test.getBlockEntity(origin.west()); alembic.addToContainer(air,8);
        var source=jar(test,origin.east(),2);
        var matrix=(InfusionMatrixBlockEntity)test.getBlockEntity(origin);
        test.assertTrue(matrix.drainEssentia(air) && source.amount()==1 && alembic.amount()==8,
                "A real matrix drain bypasses the earlier alembic and consumes the eligible jar");
        test.assertTrue(matrix.sources().size()==1 && matrix.sources().get(0).position().equals(source.getBlockPos()),
                "The animation identifies the container that actually paid the cost");
        test.succeed();
    }
    private static EssentiaJarBlockEntity jar(GameTestHelper test,BlockPos pos,int amount) {
        test.setBlock(pos,TCArcane.ESSENTIA_JAR.get());
        var jar=(EssentiaJarBlockEntity)test.getBlockEntity(pos);
        jar.addToContainer(TCAspects.catalog().require("aer"),amount); return jar;
    }
}
