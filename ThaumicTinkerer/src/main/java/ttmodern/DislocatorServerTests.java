package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class DislocatorServerTests {
    @GameTest(template="wand_test_empty")
    public static void tinkererDislocatorPreservesContainersAndRollsBack(GameTestHelper test){
        var level=test.getLevel();var a=test.absolutePos(new BlockPos(1,1,1));var b=a.offset(3,0,0);
        level.setBlock(a,Blocks.CHEST.defaultBlockState(),3);level.setBlock(b,Blocks.BARREL.defaultBlockState(),3);
        var diamond=new ItemStack(Items.DIAMOND,19);diamond.setHoverName(net.minecraft.network.chat.Component.literal("Cargo"));
        ((Container)level.getBlockEntity(a)).setItem(0,diamond);((Container)level.getBlockEntity(b)).setItem(3,new ItemStack(Items.EMERALD,7));
        var box=new AABB(a,b).inflate(2);int drops=level.getEntitiesOfClass(ItemEntity.class,box).size();
        try {
            test.assertTrue(DislocatorEntity.exchange(level,a,b),"Container exchange succeeds");
            test.assertTrue(level.getBlockState(a).is(Blocks.BARREL)&&level.getBlockState(b).is(Blocks.CHEST),"States swap");
            test.assertTrue(((Container)level.getBlockEntity(a)).getItem(3).getCount()==7&&ItemStack.matches(((Container)level.getBlockEntity(b)).getItem(0),diamond),"Both NBT inventories survive");
            test.assertTrue(level.getBlockEntity(a).getBlockPos().equals(a)&&level.getBlockEntity(b).getBlockPos().equals(b),"Coordinates reconstructed at destination");
            int[] calls={0};
            test.assertTrue(!DislocatorEntity.exchange(level,a,b,(pos,state)->++calls[0]!=2&&level.setBlock(pos,state,2|16)),"Second write rejection rolls back");
            test.assertTrue(level.getBlockState(a).is(Blocks.BARREL)&&level.getBlockState(b).is(Blocks.CHEST)&&((Container)level.getBlockEntity(a)).getItem(3).getCount()==7&&ItemStack.matches(((Container)level.getBlockEntity(b)).getItem(0),diamond),"Rollback restores both state and contents");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class,box).size()==drops,"No container contents dropped or duplicated");
            ((Container)level.getBlockEntity(a)).clearContent();((Container)level.getBlockEntity(b)).clearContent();
            level.setBlock(a,Blocks.BEDROCK.defaultBlockState(),3);
            test.assertTrue(!DislocatorEntity.exchange(level,a,b)&&level.getBlockState(a).is(Blocks.BEDROCK),"Unbreakable block stays in place");test.succeed();
        }finally{for(var pos:java.util.List.of(a,b)){level.removeBlockEntity(pos);level.removeBlock(pos,false);}}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererDislocatorPulsesQueueOnceAndMoveEntities(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(1,1,1));var local=pos.east();var remote=pos.offset(8,0,0);
        level.setBlock(pos,Tinkerer.DISLOCATOR.get().defaultBlockState().setValue(DislocatorBlock.FACING,Direction.EAST),3);
        level.setBlock(local,Blocks.STONE.defaultBlockState(),3);level.setBlock(remote,Blocks.DIRT.defaultBlockState(),3);
        var tile=(DislocatorEntity)level.getBlockEntity(pos);
        var cargo=new ItemEntity(level,remote.getX()+.5,remote.getY()+.5,remote.getZ()+.5,new ItemStack(Items.APPLE));level.addFreshEntity(cargo);
        try {
            test.assertTrue(!tile.link(pos.offset(17,0,0))&&tile.link(remote),"Sixteen axis range enforced");tile.receivePulse();
            test.assertTrue(level.getBlockState(local).is(Blocks.DIRT)&&level.getBlockState(remote).is(Blocks.STONE),"Pulse exchanges selected face and binding");
            test.assertTrue(cargo.blockPosition().equals(local)&&cargo.getItem().is(Items.APPLE),"Entity moves by exact offset without replacing its payload");
            tile.receivePulse();tile.receivePulse();
            var saved=tile.saveWithoutMetadata();tile.load(saved);
            for(int i=0;i<9;i++)tile.tick();test.assertTrue(level.getBlockState(local).is(Blocks.DIRT),"Cooldown prevents early exchange");tile.tick();
            test.assertTrue(level.getBlockState(local).is(Blocks.STONE)&&cargo.blockPosition().equals(remote),"Single stored pulse survives load and fires at tick ten");
            for(int i=0;i<20;i++)tile.tick();test.assertTrue(level.getBlockState(local).is(Blocks.STONE),"Multiple cooldown pulses coalesce to one");test.succeed();
        }finally{cargo.discard();level.removeBlock(pos,false);level.removeBlock(local,false);level.removeBlock(remote,false);}
    }
    @GameTest(template="wand_test_empty",timeoutTicks=80)
    public static void tinkererDislocatorRespondsOnlyToRedstoneEdges(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var local=pos.east();var remote=pos.offset(5,0,0);
        level.setBlock(pos,Tinkerer.DISLOCATOR.get().defaultBlockState().setValue(DislocatorBlock.FACING,Direction.EAST),3);
        level.setBlock(local,Blocks.STONE.defaultBlockState(),3);level.setBlock(remote,Blocks.DIRT.defaultBlockState(),3);
        ((DislocatorEntity)level.getBlockEntity(pos)).link(remote);level.setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        test.runAfterDelay(4,()->test.assertTrue(level.getBlockState(local).is(Blocks.DIRT),"Rising edge triggers scheduled exchange"));
        test.runAfterDelay(20,()->{
            test.assertTrue(level.getBlockState(local).is(Blocks.DIRT),"Continuous power does not retrigger");
            level.removeBlock(pos.west(),false);level.setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        });
        test.runAfterDelay(24,()->{
            test.assertTrue(level.getBlockState(local).is(Blocks.STONE),"New rising edge triggers again");
            for(var at:java.util.List.of(pos,local,remote,pos.west()))level.removeBlock(at,false);test.succeed();
        });
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererDislocatorHonorsMirrorConfiguration(GameTestHelper test){
        var option=tc4revival.config.TCConfig.SERVER.allowMirrors;boolean original=option.get();
        var research=tc4revival.research.ResearchCatalogs.catalog().require(Tinkerer.ID+":DISLOCATOR");
        var recipe=(tc4revival.content.ArcaneRecipe)test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":transvector_dislocator")).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        grid.setItem(1,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:mirrored_glass"))));grid.setItem(4,new ItemStack(Tinkerer.INTERFACE.get()));grid.setItem(7,new ItemStack(Items.COMPARATOR));
        try{
            for(boolean enabled:new boolean[]{false,true}){
                option.set(enabled);
                test.assertTrue(tc4revival.research.ResearchCatalogs.configuredEnabled(research)==enabled&&recipe.matches(grid,test.getLevel())==enabled,"Mirror configuration controls research eligibility and the actual recipe");
                // The client-facing enabled() reads a synchronized snapshot, refreshed at reload.
                var snapshot=thaumcraft.api.research.ResearchPrerequisites.materialize(tc4revival.research.ResearchDefinitions.server().json());
                test.assertTrue(snapshot.getAsJsonArray("disabledResearch").contains(new com.google.gson.JsonPrimitive(research.key()))!=enabled,"Reloaded research snapshot hides or restores dislocator research");
            }test.succeed();
        }finally{option.set(original);}
    }

}
