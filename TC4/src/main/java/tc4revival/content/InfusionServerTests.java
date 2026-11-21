package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Batched server regressions; authored without running the incomplete wand system. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class InfusionServerTests {
    private InfusionServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void altarConversionRestoresFailedWritesAndChargesOnlyCompleteStructures(GameTestHelper test) {
        var origin=new BlockPos(1,1,1); var world=test.getLevel(); var absolute=test.absolutePos(origin);
        for (var pos:BlockPos.betweenClosed(origin,origin.offset(2,2,2))) test.setBlock(pos,Blocks.AIR);
        test.setBlock(origin.offset(1,0,1),TCArcane.ARCANE_PEDESTAL.get());
        test.setBlock(origin.offset(1,2,1),TCArcane.INFUSION_MATRIX.get());
        for (int x:new int[]{0,2}) for (int z:new int[]{0,2}) {
            test.setBlock(origin.offset(x,0,z),TCArcane.ARCANE_STONE_BRICKS.get());
            test.setBlock(origin.offset(x,1,z),TCArcane.ARCANE_STONE.get());
        }
        var matrix=(InfusionMatrixBlockEntity)test.getBlockEntity(origin.offset(1,2,1));
        var pedestal=(ArcanePedestalBlockEntity)test.getBlockEntity(origin.offset(1,0,1));
        pedestal.setItem(0,new ItemStack(Items.DIAMOND));
        int[] calls={0,0};
        test.assertTrue(!InfusionAltar.convert(world,absolute,()->{ calls[1]++; return true; },
                (pos,state)->++calls[0]!=5 && world.setBlock(pos,state,2)),"Rejecting the fifth replacement aborts activation");
        test.assertTrue(calls[1]==0 && !matrix.active() && InfusionAltar.matches(world,absolute),
                "A partially written altar returns to its blueprint without charging or activating");
        test.assertTrue(!InfusionAltar.convert(world,absolute,()->false,(pos,state)->world.setBlock(pos,state,2)),
                "A failed final payment also aborts conversion");
        test.assertTrue(InfusionAltar.matches(world,absolute) && !matrix.active(),"Payment rejection restores all eight blocks");
        test.assertTrue(InfusionAltar.convert(world,absolute,()->{ calls[1]++; return true; },
                (pos,state)->world.setBlock(pos,state,2)),"The same restored blueprint can be activated successfully");
        test.assertTrue(calls[1]==1 && matrix.active(),"Only the completed altar commits one payment");
        for (int x:new int[]{0,2}) for (int z:new int[]{0,2}) {
            var base=absolute.offset(x,0,z);
            test.assertTrue(world.getBlockEntity(base) instanceof InfusionPillarBlockEntity pillar && pillar.orientation()==2+x+z/2,
                    "Each pillar retains its legacy orientation");
        }
        test.assertTrue(pedestal.getItem(0).is(Items.DIAMOND),"Conversion and rollback preserve the central pedestal inventory");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void crystalSupportsDropsAndVisAmuletIngredientsStayConnected(GameTestHelper test) {
        var pos=new BlockPos(3,3,3); var world=test.getLevel(); var absolute=test.absolutePos(pos);
        for (var face:Direction.values()) {
            var support=pos.relative(face.getOpposite()); test.setBlock(support,net.minecraft.world.level.block.Blocks.STONE);
            var state=TCArcane.crystal(ShardType.BALANCED).defaultBlockState().setValue(CrystalClusterBlock.FACING,face);
            test.setBlock(pos,state);
            test.assertTrue(state.canSurvive(world,absolute),"Each orientation uses its actual supporting face");
            test.assertTrue(state.is(InfusionLayout.STABILIZERS),"Every crafted crystal stabilizes infusion");
            var drops=net.minecraft.world.level.block.Block.getDrops(state,world,absolute,test.getBlockEntity(pos));
            for (int index=0;index<6;index++) {
                var shard=TCItems.shard(ShardType.values()[index]);
                test.assertTrue(drops.stream().filter(stack->stack.is(shard)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum()==1,"Mixed crystals return exactly one of each primal shard");
            }
            // Removing support exercises modern neighbour updates, rather than calling canSurvive alone.
            test.setBlock(support,net.minecraft.world.level.block.Blocks.AIR);
            test.assertTrue(world.getBlockState(absolute).isAir(),"Losing the selected supporting face removes the crystal");
        }
        var recipe=(InfusionRecipe)world.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/vis_amulet")).orElseThrow();
        var parts=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        parts.add(new net.minecraft.world.item.ItemStack(TCItems.PRIMAL_CHARM.get())); parts.add(new net.minecraft.world.item.ItemStack(TCItems.PRIMAL_CHARM.get()));
        for (int index=0;index<4;index++) parts.add(new net.minecraft.world.item.ItemStack(TCItems.crystal(ShardType.BALANCED)));
        var input=new InfusionRecipe.Input(new net.minecraft.world.item.ItemStack(TCItems.bauble(MundaneBaubleType.AMULET)),parts,key->true);
        test.assertTrue(recipe.matches(input,world) && recipe.assemble(input,world.registryAccess()).is(TCItems.VIS_AMULET.get()),"Two charms and four mixed clusters complete the old advanced amulet recipe");
        parts.set(5,new net.minecraft.world.item.ItemStack(TCItems.crystal(ShardType.AIR)));
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(input.getItem(0),parts,key->true),world),"An elemental cluster cannot replace the mixed cluster"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void intakeHintsRefreshWithoutDuplicatingOrPersistingTransactions(GameTestHelper test) {
        var pos=new BlockPos(4,3,2); var sourcePos=new BlockPos(2,2,2); var pedestalPos=new BlockPos(3,1,3);
        test.setBlock(pos,TCArcane.INFUSION_MATRIX.get()); test.setBlock(sourcePos,TCArcane.ESSENTIA_JAR.get()); test.setBlock(pedestalPos,TCArcane.ARCANE_PEDESTAL.get());
        var matrix=(InfusionMatrixBlockEntity)test.getBlockEntity(pos); var jar=(EssentiaJarBlockEntity)test.getBlockEntity(sourcePos);
        var air=TCAspects.catalog().require("aer"); jar.addToContainer(air,3);
        test.assertTrue(matrix.drainEssentia(air) && matrix.drainEssentia(air),"Both source refreshes must represent successful real drains");
        matrix.ingredientStarted(test.absolutePos(pedestalPos)); matrix.ingredientStarted(test.absolutePos(pedestalPos));
        test.assertTrue(matrix.sources().size()==2 && jar.amount()==1,"Refreshes replace visual hints while each successful drain still costs one unit");
        long now=test.getLevel().getGameTime();
        for (var source:matrix.sources()) test.assertTrue(source.expires()==now+(source.essentia()?15:60),"Source kinds retain their distinct 15/60-tick visual lifetimes");
        var display=matrix.getUpdateTag();
        test.assertTrue(!display.contains("Process"),"Visual synchronization must not serialize the live crafting transaction");
        var replica=new InfusionMatrixBlockEntity(test.absolutePos(pos),matrix.getBlockState()); replica.load(display);
        test.assertTrue(replica.sources().equals(matrix.sources()),"Coordinates, source kind, color and expiration survive client display serialization");
        var restored=new InfusionMatrixBlockEntity(test.absolutePos(pos),matrix.getBlockState()); restored.load(matrix.saveWithoutMetadata());
        test.assertTrue(restored.sources().isEmpty(),"Visual intake hints must not be replayed after loading a saved world"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void jarFilterSurvivesDrainAndPortableRoundTrip(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis");
        var pos=new BlockPos(2,2,2); test.setBlock(pos,TCArcane.ESSENTIA_JAR.get());
        var jar=(EssentiaJarBlockEntity)test.getBlockEntity(pos); jar.label(air,Direction.WEST);
        test.assertTrue(jar.suction(Direction.UP)==64 && !jar.accepts(fire),"A label controls suction and accepted phial type");
        test.assertTrue(jar.addToContainer(air,70)==6 && jar.amount()==64,"An ordinary jar returns overflow");
        test.assertTrue(jar.takeFromContainer(air,64) && jar.aspect()==null && jar.filter().equals(air),"Draining clears content identity but retains filter");
        var carried=jar.asItem(); test.assertTrue(carried.is(TCItems.FILLED_ESSENTIA_JAR.get()),"An empty labeled jar remains a portable filled-form item");
        var restored=new EssentiaJarBlockEntity(BlockPos.ZERO,jar.getBlockState()); restored.readItem(carried);
        test.assertTrue(restored.amount()==0 && restored.filter().equals(air) && restored.suction(Direction.UP)==64,"Placement restores empty filtered suction");
        restored.addToContainer(air,8); restored.load(restored.saveWithoutMetadata());
        test.assertTrue(restored.contains(air,8) && !restored.contains(fire,1),"Save/load preserves typed contents");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void voidOverflowAndRemoteSupply(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var pos=new BlockPos(2,2,2); var matrixPos=new BlockPos(4,3,2);
        test.setBlock(pos,TCArcane.VOID_JAR.get()); test.setBlock(matrixPos,TCArcane.INFUSION_MATRIX.get());
        var jar=(EssentiaJarBlockEntity)test.getBlockEntity(pos); jar.label(air,Direction.NORTH);
        test.assertTrue(jar.addToContainer(air,Integer.MAX_VALUE)==0 && jar.amount()==64,"Void overflow must be discarded without integer overflow");
        test.assertTrue(jar.suction(Direction.UP)==32 && jar.minimumSuction()==48,"Full filtered void jars retain their lower suction");
        var matrix=(InfusionMatrixBlockEntity)test.getBlockEntity(matrixPos);
        test.assertTrue(matrix.drainEssentia(air) && jar.amount()==63,"The matrix must draw one real unit from a discovered jar");
        test.assertTrue(jar.suction(Direction.UP)==48,"Vacancy restores filtered void suction");
        test.assertTrue(matrix.sources().size()==1 && matrix.sources().get(0).essentia(),"A drain publishes its source for client animation");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void altarBlueprintRequiresClearInterior(GameTestHelper test) {
        var origin=new BlockPos(1,1,1);
        for (var pos:BlockPos.betweenClosed(origin,origin.offset(2,2,2))) test.setBlock(pos,Blocks.AIR);
        test.setBlock(origin.offset(1,0,1),TCArcane.ARCANE_PEDESTAL.get()); test.setBlock(origin.offset(1,2,1),TCArcane.INFUSION_MATRIX.get());
        for (int x:new int[]{0,2}) for (int z:new int[]{0,2}) { test.setBlock(origin.offset(x,0,z),TCArcane.ARCANE_STONE_BRICKS.get()); test.setBlock(origin.offset(x,1,z),TCArcane.ARCANE_STONE.get()); }
        test.assertTrue(InfusionAltar.matches(test.getLevel(),test.absolutePos(origin)),"The legacy altar blueprint must match");
        test.setBlock(origin.offset(1,1,1),Blocks.STONE);
        test.assertTrue(!InfusionAltar.matches(test.getLevel(),test.absolutePos(origin)),"An occupied gap prevents conversion");
        test.setBlock(origin.offset(1,1,1),Blocks.AIR); test.setBlock(origin,TCArcane.ARCANE_STONE.get());
        test.assertTrue(!InfusionAltar.matches(test.getLevel(),test.absolutePos(origin)),"Lower corners require bricks, not plain stone"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void infusionResumesCostsAndConsumesIngredientsBeforePublishing(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var pedestalPos=new BlockPos(2,2,2);
        test.setBlock(pedestalPos,TCArcane.ARCANE_PEDESTAL.get()); var pedestal=(ArcanePedestalBlockEntity)test.getBlockEntity(pedestalPos);
        pedestal.setItem(0,new ItemStack(Items.WATER_BUCKET));
        var ingredient=new InfusionIngredient(com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:water_bucket\"}}").getAsJsonObject());
        var process=new InfusionProcess(); var plan=new InfusionRecipe.Plan(List.of(ingredient),new AspectAmounts().add(air,2),0,0,false,InfusionOutput.item(new ItemStack(Items.DIAMOND)));
        process.start(plan,new ItemStack(Items.IRON_INGOT),"crafter",-100);
        var random=RandomSource.create(9);
        class Host implements InfusionProcess.Host {
            int drained,started,finished; ItemStack result=ItemStack.EMPTY;
            @Override public boolean centerMatches(ItemStack expected) { return expected.is(Items.IRON_INGOT); }
            @Override public InfusionProcess.Experience drainExperience() { throw new AssertionError("Ordinary infusion must not drain experience"); }
            @Override public boolean drainEssentia(Aspect aspect) { test.assertTrue(aspect.equals(air),"Only the requested aspect is consumed"); drained++; return true; }
            @Override public List<ArcanePedestalBlockEntity> pedestals() { return List.of(pedestal); }
            @Override public void ingredientStarted(BlockPos pos) { started++; }
            @Override public void instabilityEvent(int event) { throw new AssertionError("A stable craft must not fail"); }
            @Override public void refreshSurroundings() {}
            @Override public void failed() { throw new AssertionError("Center remains valid"); }
            @Override public void finish(InfusionOutput output,String player) {
                finished++; result=output.apply(new ItemStack(Items.IRON_INGOT)); test.assertTrue(player.equals("crafter"),"Craft attribution survives reload");
                test.assertTrue(pedestal.getItem(0).is(Items.BUCKET),"Ingredient remainder precedes output publication");
                process.cycle(random,this);
            }
            @Override public void changed() {}
        }
        var host=new Host(); process.cycle(random,host); process.load(process.save());
        test.assertTrue(process.remaining().amount(air)==1,"Reload must not refill the consumed cost");
        process.cycle(random,host);
        for (int cycle=0;cycle<6;cycle++) process.cycle(random,host);
        test.assertTrue(host.finished==0 && pedestal.getItem(0).is(Items.BUCKET),"The six ingredient cycles consume before completion");
        process.cycle(random,host); process.cycle(random,host);
        test.assertTrue(!process.crafting() && host.finished==1 && host.drained==2 && host.started==1 && host.result.is(Items.DIAMOND),"The transaction must finish once, including during reentrant callbacks");
        tc4revival.TC4Revival.LOGGER.info("PASS infusionResumesCostsAndConsumesIngredientsBeforePublishing");
        test.succeed();
    }
}
