package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class EssentiaCrystallizerServerTests {
    private static EssentiaCrystallizerBlockEntity machine(GameTestHelper test,Direction face){
        var pos=test.absolutePos(new BlockPos(4,4,4));test.getLevel().setBlockAndUpdate(pos,TCArcane.ESSENTIA_CRYSTALLIZER.get().defaultBlockState().setValue(EssentiaCrystallizerBlock.FACING,face));return (EssentiaCrystallizerBlockEntity)test.getLevel().getBlockEntity(pos);
    }
    private static void ticks(EssentiaCrystallizerBlockEntity tile,int count){for(int i=0;i<count;i++)tile.tick();}
    @GameTest(template="wand_test_empty")
    public static void crystallizerTransportAndPause(GameTestHelper test){
        var level=test.getLevel();var tile=machine(test,Direction.WEST);var pos=tile.getBlockPos();var air=TCAspects.catalog().require("aer");
        test.assertTrue(tile.suction(Direction.WEST)==128 && tile.suction(Direction.EAST)==64 && tile.connectable(Direction.WEST)&&!tile.connectable(Direction.EAST),"Empty input face draws at 128, other queried faces retain legacy 64");
        level.setBlockAndUpdate(pos.west(),TCArcane.ESSENTIA_TUBE.get().defaultBlockState());var tube=(EssentiaTubeBlockEntity)level.getBlockEntity(pos.west());tube.addEssentia(air,1,Direction.EAST);
        ticks(tile,5);test.assertTrue(tile.amount()==1 && tube.essentiaAmount(Direction.EAST)==0 && tile.progress()==0,"Pulls one unit before starting processing");
        ticks(tile,5);test.assertTrue(tile.progress()==1 && tile.suction(Direction.WEST)==64,"Unpowered processing advances once per five ticks");
        level.setBlockAndUpdate(pos.north(),Blocks.REDSTONE_BLOCK.defaultBlockState());ticks(tile,20);
        test.assertTrue(tile.progress()==1 && tile.suction(Direction.WEST)==0,"Redstone pauses processing and suppresses suction");level.setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
        var restored=new EssentiaCrystallizerBlockEntity(pos,tile.getBlockState());restored.load(tile.saveWithoutMetadata());
        test.assertTrue(restored.aspect().equals(air)&&restored.progress()==1,"Aspect and unfinished progress persist");
        test.assertTrue(!tile.takeFromContainer(TCAspects.catalog().require("ignis"),1) && tile.amount()==1,"Wrong aspect cannot withdraw");
        test.assertTrue(tile.takeFromContainer(air,1) && tile.amount()==0 && tile.progress()==0,"Correct withdrawal resets processing");
        test.assertTrue(tile.addEssentia(air,1,Direction.EAST)==0 && tile.addEssentia(air,2,Direction.WEST)==1 && tile.takeEssentia(air,1,Direction.WEST)==0,"Only the input accepts a single unit; transport cannot extract");
        pass(test,"transport-redstone-save");
    }
    @GameTest(template="wand_test_empty")
    public static void crystallizerOutputConservation(GameTestHelper test){
        var level=test.getLevel();var tile=machine(test,Direction.WEST);var pos=tile.getBlockPos();var air=TCAspects.catalog().require("aer");
        level.setBlockAndUpdate(pos.east(),Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos.east());
        tile.addToContainer(air,1);ticks(tile,995);test.assertTrue(tile.amount()==1 && chest.isEmpty() && tile.progress()==199,"Crystal is not emitted before the 200th processing step");ticks(tile,5);
        test.assertTrue(tile.amount()==0 && chest.getItem(0).is(TCItems.ESSENCE_CRYSTAL.get()) && TCItems.ESSENCE_CRYSTAL.get().storedAspects(chest.getItem(0)).amount(air)==1,"Completion inserts exactly one correctly tagged crystal into output inventory");
        for(int slot=0;slot<chest.getContainerSize();slot++)chest.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=event->{if(event.getLevel()==level && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity)event.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(reject);
        try{tile.addToContainer(air,1);ticks(tile,1000);test.assertTrue(tile.amount()==1 && tile.progress()>=200,"Rejected world output preserves the pending unit");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(reject);}
        tile.tick();var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        test.assertTrue(tile.amount()==0 && drops.size()==1 && drops.get(0).getItem().getCount()==1 && Math.abs(drops.get(0).getDeltaMovement().x-.04)<1e-6,"A full output container falls back to one directed world item without loss or duplication");drops.forEach(net.minecraft.world.entity.Entity::discard);
        pass(test,"output-time-conservation");
    }
    @GameTest(template="wand_test_empty")
    public static void crystallizerEarthVisAndRecipe(GameTestHelper test){
        var level=test.getLevel();var tile=machine(test,Direction.DOWN);var nodePos=tile.getBlockPos().east(3);var earth=TCAspects.catalog().require("terra");
        level.setBlockAndUpdate(nodePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState());var node=(EnergizedNodeBlockEntity)level.getBlockEntity(nodePos);
        node.configure(new AspectAmounts().add(earth,400),AuraNodeType.NORMAL,null,"crystallizer-earth");node.tick();tile.addToContainer(TCAspects.catalog().require("aer"),1);ticks(tile,5);
        test.assertTrue(tile.progress()==41,"Twenty earth CV add forty steps alongside base progress");level.setBlockAndUpdate(nodePos,Blocks.AIR.defaultBlockState());
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){@Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);Item[] items={Items.IRON_INGOT,Items.DISPENSER,Items.IRON_INGOT,TCItems.shard(ShardType.BALANCED),TCItems.ALCHEMICAL_CONSTRUCT.get(),TCItems.shard(ShardType.BALANCED),Items.OAK_PLANKS,TCArcane.ESSENTIA_TUBE.get().asItem(),Items.BIRCH_PLANKS};
        for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(items[i]));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(value->value.getId().getPath().endsWith("essentia_crystallizer")).findFirst().orElseThrow();
        test.assertTrue(recipe.matches(grid,level) && recipe.assemble(grid,level.registryAccess()).is(TCItems.ESSENTIA_CRYSTALLIZER.get()) && recipe.visCost(grid).get(earth)==15 && recipe.visCost(grid).values().stream().mapToInt(Integer::intValue).sum()==25,"Actual arcane recipe accepts mixed planks and uses 5 water, 15 earth, 5 order vis");
        pass(test,"earth-vis-and-recipe");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Crystallizer test passed: {}",name);test.succeed();}
}
