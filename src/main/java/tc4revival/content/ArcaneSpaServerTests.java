package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ArcaneSpaServerTests {
    private static ArcaneSpaBlockEntity spa(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){level.setBlockAndUpdate(pos.offset(x,0,z),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.offset(x,1,z),Blocks.AIR.defaultBlockState());}
        level.setBlockAndUpdate(pos,TCArcane.ARCANE_SPA.get().defaultBlockState());return (ArcaneSpaBlockEntity)level.getBlockEntity(pos);
    }
    private static void ticks(ArcaneSpaBlockEntity spa,int count) { for(int i=0;i<count;i++)spa.tick(); }
    @GameTest(template="wand_test_empty")
    public static void arcaneSpaMixTimingAndPool(GameTestHelper test) {
        var spa=spa(test);var level=test.getLevel();var pos=spa.getBlockPos();spa.tank().fill(new FluidStack(Fluids.WATER,5000),FluidAction.EXECUTE);spa.setItem(0,new ItemStack(TCCleansing.BATH_SALTS.get(),40));spa.tick();
        test.assertTrue(level.getBlockState(pos.above()).is(TCCleansing.PURE_WATER.get())&&spa.tank().getFluidAmount()==4000&&spa.getItem(0).getCount()==39,"First tick consumes one salt and a bucket of water to create a purifying source");
        ticks(spa,39);test.assertTrue(spa.tank().getFluidAmount()==4000,"No second output during the remaining thirty-nine ticks");spa.tick();
        test.assertTrue(level.getBlockState(pos.above().west()).is(TCCleansing.PURE_WATER.get())&&spa.tank().getFluidAmount()==3000,"Expansion follows X-then-Z scan order with six-face source adjacency");
        level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());ticks(spa,80);test.assertTrue(spa.tank().getFluidAmount()==3000,"Redstone pauses output");level.setBlockAndUpdate(pos.east(),Blocks.STONE.defaultBlockState());
        for(int i=0;i<1200;i++){spa.tank().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);spa.tick();}
        int sources=0;for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(level.getBlockState(pos.offset(x,1,z)).is(TCCleansing.PURE_WATER.get()))sources++;
        test.assertTrue(sources==25&&spa.getItem(0).getCount()==15,"Exactly the five-by-five pool fills, consuming twenty-five salt total");
        int remaining=spa.tank().getFluidAmount();ticks(spa,80);test.assertTrue(spa.tank().getFluidAmount()==remaining,"Full pool cannot consume more ingredients");pass(test,"mix-time-redstone-pool");
    }
    @GameTest(template="wand_test_empty")
    public static void arcaneSpaFluidsSupportAndSave(GameTestHelper test) {
        var spa=spa(test);var level=test.getLevel();var pos=spa.getBlockPos();spa.tank().fill(new FluidStack(Fluids.LAVA,2000),FluidAction.EXECUTE);spa.setItem(0,new ItemStack(TCCleansing.BATH_SALTS.get(),2));spa.tick();test.assertTrue(level.isEmptyBlock(pos.above()),"Mixing requires water even when other fluids fit the tank");
        spa.toggleMix();ticks(spa,40);test.assertTrue(level.getBlockState(pos.above()).is(Blocks.LAVA)&&spa.tank().getFluidAmount()==1000&&spa.getItem(0).getCount()==2,"Plain mode places lava without salt");
        var saved=spa.saveWithoutMetadata();var restored=new ArcaneSpaBlockEntity(pos,spa.getBlockState());restored.load(saved);test.assertTrue(!restored.mix()&&restored.tank().getFluidAmount()==1000&&restored.getItem(0).getCount()==2,"Mode, fluid and salt inventory persist");
        spa.tank().drain(5000,FluidAction.EXECUTE);spa.tank().fill(new FluidStack(TCFluids.DEATH.get(),1000),FluidAction.EXECUTE);level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());ticks(spa,40);
        test.assertTrue(level.getBlockState(pos.above()).is(TCArcane.LIQUID_DEATH.get())&&level.getBlockState(pos.above()).getValue(FiniteFluidBlock.QUANTA)==4&&spa.tank().isEmpty(),"Finite fluid output receives full legacy quanta");
        level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());spa.tank().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(spa,40);test.assertTrue(spa.tank().getFluidAmount()==1000,"Blocked center consumes no fluid");
        level.setBlockAndUpdate(pos.above(),Blocks.WATER.defaultBlockState());for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)level.setBlockAndUpdate(pos.offset(x,0,z),Blocks.AIR.defaultBlockState());ticks(spa,40);test.assertTrue(spa.tank().getFluidAmount()==1000,"Expansion requires solid support under the destination");pass(test,"fluid-modes-support-save");
    }
    @GameTest(template="wand_test_empty")
    public static void arcaneSpaContainersAutomationAndMenu(GameTestHelper test) {
        var spa=spa(test);var level=test.getLevel();var pos=spa.getBlockPos();var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SpaTest"));player.setPos(pos.getCenter());player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        spa.tank().fill(new FluidStack(Fluids.WATER,4750),FluidAction.EXECUTE);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        spa.getBlockState().use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        test.assertTrue(spa.tank().getFluidAmount()==5000&&player.getInventory().countItem(Items.WATER_BUCKET)==0&&player.getInventory().countItem(Items.BUCKET)==1,"Partial manual refill consumes whole bucket even in creative and returns the empty vessel");
        for(var face:Direction.values()) {
            var fluid=spa.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,face);
            test.assertTrue(fluid.isPresent()==(face!=Direction.UP),"Fluid automation only exposes side and bottom");
            var item=spa.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,face).orElseThrow(()->new AssertionError("No item port"));test.assertTrue(item.getSlots()==(face==Direction.UP?0:1),"Top exposes no inventory slot");
            if(face!=Direction.UP) { test.assertTrue(item.insertItem(0,new ItemStack(Items.DIRT),false).getCount()==1,"Only salts enter through automation");test.assertTrue(item.insertItem(0,new ItemStack(TCCleansing.BATH_SALTS.get()),false).isEmpty()&&item.extractItem(0,1,false).getCount()==1,"Side and bottom insert and extract salt"); }
        }
        var menu=new ArcaneSpaMenu(1,player.getInventory(),spa);var previous=player.containerMenu;test.assertTrue(!menu.clickMenuButton(player,1),"Unopened menu cannot toggle mode");player.containerMenu=menu;
        try { test.assertTrue(menu.clickMenuButton(player,1)&&!spa.mix()&&!menu.clickMenuButton(player,2),"Valid button toggles mixing");player.setPos(pos.getCenter().add(20,0,0));test.assertTrue(!menu.clickMenuButton(player,1),"Distant menu cannot toggle"); } finally { player.containerMenu=previous; }
        pass(test,"container-automation-menu");
    }
    @GameTest(template="wand_test_empty")
    public static void arcaneSpaRecipeAndNether(GameTestHelper test) {
        var spa=spa(test);var level=test.getLevel();var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);Item[] items={Items.QUARTZ_BLOCK,Items.IRON_BARS,Items.QUARTZ_BLOCK,TCItems.ARCANE_STONE.get(),TCItems.ESSENTIA_JAR.get(),TCItems.ARCANE_STONE.get(),TCItems.ARCANE_STONE.get(),Items.PISTON,TCItems.ARCANE_STONE.get()};for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(items[i]));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("arcane_spa")).findFirst().orElseThrow();test.assertTrue(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(TCItems.ARCANE_SPA.get())&&recipe.visCost(grid).values().stream().mapToInt(Integer::intValue).sum()==28&&recipe.unlocked(grid,k->k.equals("ARCANESPA"))&&!recipe.unlocked(grid,k->false),"Actual spa recipe and research gate");
        var nether=level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);var pos=new BlockPos(spa.getBlockPos().getX(),100,spa.getBlockPos().getZ());var old=nether.getBlockState(pos);var above=nether.getBlockState(pos.above());
        try { nether.setBlockAndUpdate(pos,TCArcane.ARCANE_SPA.get().defaultBlockState());nether.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());var hot=(ArcaneSpaBlockEntity)nether.getBlockEntity(pos);hot.toggleMix();hot.tank().fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE);hot.tick();test.assertTrue(nether.isEmptyBlock(pos.above())&&hot.tank().getFluidAmount()==2000,"Plain water cannot be placed or consumed in the Nether");hot.toggleMix();hot.setItem(0,new ItemStack(TCCleansing.BATH_SALTS.get()));ticks(hot,40);test.assertTrue(nether.getBlockState(pos.above()).is(TCCleansing.PURE_WATER.get())&&hot.tank().getFluidAmount()==1000,"Purifying water retains the old Nether exemption"); } finally { nether.setBlockAndUpdate(pos,old);nether.setBlockAndUpdate(pos.above(),above); }
        pass(test,"recipe-nether");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Arcane spa test passed: {}",name);test.succeed(); }
}
