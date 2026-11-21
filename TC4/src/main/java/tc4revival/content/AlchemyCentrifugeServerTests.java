package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class AlchemyCentrifugeServerTests {
    private static AlchemyCentrifugeBlockEntity machine(GameTestHelper test){var pos=test.absolutePos(new BlockPos(4,4,4));test.getLevel().setBlockAndUpdate(pos,TCArcane.ALCHEMY_CENTRIFUGE.get().defaultBlockState());return (AlchemyCentrifugeBlockEntity)test.getLevel().getBlockEntity(pos);}
    private static void ticks(AlchemyCentrifugeBlockEntity tile,int amount){for(int i=0;i<amount;i++)tile.tick();}
    @GameTest(template="wand_test_empty")
    public static void centrifugeFlowTimingAndPause(GameTestHelper test){
        var level=test.getLevel();var tile=machine(test);var pos=tile.getBlockPos();var compound=TCAspects.catalog().require("victus");var primal=TCAspects.catalog().require("aer");
        level.setBlockAndUpdate(pos.below(),TCArcane.ESSENTIA_TUBE.get().defaultBlockState());var tube=(EssentiaTubeBlockEntity)level.getBlockEntity(pos.below());tube.addEssentia(primal,1,Direction.UP);ticks(tile,5);
        test.assertTrue(tile.input()==null&&tube.essentiaAmount(Direction.UP)==1,"Primal essentia is not consumed");tube.takeEssentia(primal,1,Direction.UP);tube.addEssentia(compound,1,Direction.UP);ticks(tile,5);
        test.assertTrue(tile.input().equals(compound)&&tile.process()==38&&tube.essentiaAmount(Direction.UP)==0,"One compound enters from below every five idle ticks, with processing beginning on that tick");
        level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());ticks(tile,50);test.assertTrue(tile.process()==38&&tile.suction(Direction.DOWN)==0,"Redstone freezes processing and suction");level.setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
        ticks(tile,37);test.assertTrue(tile.input()!=null&&tile.amount()==0,"No early output");tile.tick();
        test.assertTrue(tile.input()==null&&tile.amount()==1&&compound.components().contains(tile.aspect().tag()),"Exactly one component is selected after the remaining processing steps");
        tube.addEssentia(compound,1,Direction.UP);ticks(tile,10);test.assertTrue(tile.input()==null&&tube.essentiaAmount(Direction.UP)==1,"Blocked output prevents automatic input draw");
        test.assertTrue(tile.suction(Direction.DOWN)==128&&tile.suction(Direction.UP)==0&&!tile.connectable(Direction.NORTH),"Idle suction follows input state even with output full; ports stay vertical");
        test.assertTrue(tile.takeEssentia(tile.aspect(),1,Direction.DOWN)==0&&tile.takeEssentia(tile.aspect(),1,Direction.UP)==1,"Only the top permits extraction");pass(test,"flow-timing-redstone");
    }
    @GameTest(template="wand_test_empty")
    public static void centrifugeRandomComponentsAndConservation(GameTestHelper test){
        var level=test.getLevel();var tile=machine(test);var compound=TCAspects.catalog().require("victus");var observed=new java.util.HashSet<String>();
        test.assertTrue(tile.addEssentia(compound,0,Direction.DOWN)==0&&tile.input()==null&&tile.addEssentia(compound,-1,Direction.DOWN)==0,"Nonpositive input cannot manufacture essentia");
        for(int sample=0;sample<64;sample++){
            level.random.setSeed(sample*104729L);test.assertTrue(tile.addEssentia(compound,10,Direction.WEST)==0&&tile.addEssentia(compound,10,Direction.DOWN)==1,"Only the lower input accepts one unit");
            ticks(tile,39);var output=tile.aspect();test.assertTrue(output!=null&&compound.components().contains(output.tag()),"Every output is one of the two immediate components");observed.add(output.tag());
            test.assertTrue(tile.takeEssentia(output,0,Direction.UP)==0&&tile.amount()==1,"Zero withdrawal cannot remove output");
            test.assertTrue(tile.takeEssentia(output,64,Direction.UP)==0&&tile.amount()==1,"Oversized atomic withdrawal is rejected");
            test.assertTrue(tile.takeEssentia(output,1,Direction.UP)==1&&tile.amount()==0,"Exact withdrawal conserves the stored unit");
        }
        test.assertTrue(observed.size()==2,"Seeded sample exercises both random branches");
        tile.addEssentia(compound,1,Direction.DOWN);tile.addToContainer(TCAspects.catalog().require("aer"),1);ticks(tile,39);
        test.assertTrue(tile.input()!=null&&tile.process()==0&&tile.aspect().tag().equals("aer"),"Direct container insertion fills output while an input waits without overwriting either");
        var saved=tile.saveWithoutMetadata();var restored=new AlchemyCentrifugeBlockEntity(tile.getBlockPos(),tile.getBlockState());restored.setLevel(level);restored.load(saved);
        test.assertTrue(restored.input().equals(compound)&&restored.aspect().tag().equals("aer")&&restored.process()==0,"Both reservoirs persist but process counter does not");
        restored.takeFromContainer(restored.aspect(),1);restored.tick();test.assertTrue(restored.input()==null&&restored.amount()==1,"Loaded unfinished work completes on the next unpowered tick after output is free");
        saved.putString("aspectIn","aer");restored.load(saved);restored.tick();test.assertTrue(restored.input()==null,"Malformed primal input cannot crash component selection");pass(test,"random-conservation-save");
    }
    @GameTest(template="wand_test_empty")
    public static void centrifugeRecipeAndGeometry(GameTestHelper test){
        var level=test.getLevel();var tile=machine(test);var state=tile.getBlockState();var shape=state.getShape(level,tile.getBlockPos()).bounds();
        test.assertTrue(shape.minX==.25&&shape.maxX==.75&&state.getCollisionShape(level,tile.getBlockPos()).bounds().getXsize()==.5,"Collision follows the actual narrow device shape");
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){@Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);grid.setItem(1,new ItemStack(TCArcane.ESSENTIA_TUBE.get()));grid.setItem(3,new ItemStack(TCItems.ALEMBIC.get()));grid.setItem(4,new ItemStack(TCItems.ALCHEMICAL_CONSTRUCT.get()));grid.setItem(5,new ItemStack(Items.PISTON));grid.setItem(7,new ItemStack(TCArcane.ESSENTIA_TUBE.get()));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(value->value.getId().getPath().endsWith("alchemy_centrifuge")).findFirst().orElseThrow();
        test.assertTrue(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(TCItems.ALCHEMY_CENTRIFUGE.get())&&recipe.visCost(grid).size()==3&&recipe.visCost(grid).values().stream().allMatch(cost->cost==5),"Actual recipe has three five-vis costs and correct ingredients");pass(test,"recipe-geometry");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Centrifuge test passed: {}",name);test.succeed();}
}
