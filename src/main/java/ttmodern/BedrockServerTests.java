package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class BedrockServerTests {
    @GameTest(template="wand_test_empty") public static void awakenedPickInstantSpeedOnlyForGeneratedBedrockOres(GameTestHelper test){
        var mining=test.getLevel().getServer().getLevel(BedrockTravel.DIMENSION);
        var player=new net.minecraftforge.common.util.FakePlayer(mining,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"OreSpeed"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Tinkerer.AWAKENED_PICK.get()));
        var ores=BedrockOreFeature.generatedOres(mining.registryAccess());test.assertTrue(!ores.isEmpty(),"Generated ore set is available for client synchronization");
        for(var id:ores){
            var state=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
            var event=new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(player,state,10,BlockPos.ZERO);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            test.assertTrue(event.getNewSpeed()==Float.MAX_VALUE,"Every generated ore gets instant speed: "+id);
        }
        for(var block:java.util.List.of(Blocks.OBSIDIAN,Blocks.STONE,Blocks.DEEPSLATE_IRON_ORE)){
            var event=new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(player,block.defaultBlockState(),10,BlockPos.ZERO);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);test.assertTrue(event.getNewSpeed()!=Float.MAX_VALUE,"Non-generated blocks keep normal speed");
        }
        var other=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"OtherOreSpeed"));
        other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,player.getMainHandItem().copy());
        var event=new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(other,Blocks.IRON_ORE.defaultBlockState(),10,BlockPos.ZERO);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);test.assertTrue(event.getNewSpeed()!=Float.MAX_VALUE,"Overworld ore keeps normal speed");test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400) public static void tinkererBedrockModernFloorEntryAndReturn(GameTestHelper test){
        var world=test.getLevel();var at=test.absolutePos(new BlockPos(2,1,2));var floor=new BlockPos(at.getX(),world.getMinBuildHeight(),at.getZ());var high=floor.above(6);
        var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTRoundTrip"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setShiftKeyDown(true);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Tinkerer.AWAKENED_PICK.get()));
        boolean enabled=TinkererConfig.BEDROCK_ENABLED.get();var mining=world.getServer().getLevel(BedrockTravel.DIMENSION);test.assertTrue(mining!=null,"Bedrock dimension loaded");var landing=new BlockPos(floor.getX(),249,floor.getZ());mining.getChunkAt(landing);
        try {
            TinkererConfig.BEDROCK_ENABLED.set(true);world.setBlockAndUpdate(high,Blocks.BEDROCK.defaultBlockState());player.moveTo(high.above().getCenter());
            var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,high,net.minecraft.core.Direction.UP);BedrockTravel.click(event);test.assertTrue(world.getBlockState(high).is(Blocks.BEDROCK),"Overworld entry is restricted to the modern bottom five layers");
            world.setBlockAndUpdate(floor,Blocks.BEDROCK.defaultBlockState());player.moveTo(floor.above().getCenter());event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,floor,net.minecraft.core.Direction.UP);BedrockTravel.click(event);test.assertTrue(world.getBlockState(floor).is(Tinkerer.BEDROCK_PORTAL.get()),"Sneak mining at negative minimum Y opens the portal");
            for(int i=0;i<=5;i++)mining.setBlockAndUpdate(landing.above(i),Blocks.BEDROCK.defaultBlockState());
            test.assertTrue(BedrockTravel.travel(player,floor)&&player.serverLevel()==mining&&player.getY()==251,"Real server teleport enters the mining dimension");
            test.assertTrue(mining.getBlockState(landing.above(5)).is(Tinkerer.BEDROCK_PORTAL.get()),"Entry creates a usable return portal");
            TinkererConfig.BEDROCK_ENABLED.set(false);player.setPortalCooldown(0);test.assertTrue(BedrockTravel.travel(player,landing.above(5))&&player.serverLevel()==world,"Disabling new entry never traps a player in the dimension");
            test.assertTrue(world.getBlockState(player.blockPosition().below()).isFaceSturdy(world,player.blockPosition().below(),net.minecraft.core.Direction.UP),"Return finds a solid overworld landing");test.succeed();
        } finally {TinkererConfig.BEDROCK_ENABLED.set(enabled);world.setBlockAndUpdate(floor,Blocks.BEDROCK.defaultBlockState());world.removeBlock(high,false);player.discard();}
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400) public static void tinkererBedrockDimensionGeneratesWeightedOreColumns(GameTestHelper test){
        var world=test.getLevel().getServer().getLevel(BedrockTravel.DIMENSION);test.assertTrue(world!=null,"Addon-owned bedrock dimension is loaded");test.assertTrue(world.getMinBuildHeight()==0&&world.getMaxBuildHeight()==256,"Original vertical mining range retained independently from overworld");int ores=0,bedrock=0;
        // Legacy veins offset their centers by eight blocks and may lie across a chunk boundary.
        for(int cx=186;cx<=188;cx++)for(int cz=192;cz<=194;cz++)world.getChunk(cx,cz);
        for(int cx=186;cx<=188;cx++)for(int cz=192;cz<=194;cz++){
            var chunk=world.getChunk(cx,cz);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<256;y++){var state=chunk.getBlockState(new BlockPos((cx<<4)+x,y,(cz<<4)+z));if(state.is(Blocks.BEDROCK))bedrock++;else if(state.is(net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse("forge:ores"))))ores++;}
        }
        test.assertTrue(bedrock>450000&&ores>0,"Flat bedrock and configured ore feature execute: bedrock="+bedrock+", ores="+ores);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererBedrockVanillaOreSurvivesOptionalGenerationFilters(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(2,5,2));var center=origin.offset(8,-1,8);
        var bounds=BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4));
        var previous=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try{
            for(var p:bounds){previous.put(p.immutable(),level.getBlockState(p));level.setBlock(p,Blocks.BEDROCK.defaultBlockState(),2);}
            level.setBlock(center,Blocks.STONE.defaultBlockState(),2);
            test.assertTrue(!BedrockOreFeature.placeVein(level,net.minecraft.util.RandomSource.create(42),origin,Blocks.IRON_ORE.defaultBlockState(),0),"Empty vein does not mutate the world");
            test.assertTrue(BedrockOreFeature.placeVein(level,net.minecraft.util.RandomSource.create(42),origin,Blocks.IRON_ORE.defaultBlockState(),19),"Dimension-specific iron deposits survive GTCEu vanilla-ore filtering");
            test.assertTrue(level.getBlockState(center).is(Blocks.STONE),"Veins replace bedrock only");
            int ores=0;for(var p:bounds)if(level.getBlockState(p).is(Blocks.IRON_ORE))ores++;
            test.assertTrue(ores>0&&ores<previous.size(),"Finite vein preserves the surrounding bedrock");test.succeed();
        }finally{previous.forEach((p,state)->level.setBlock(p,state,2));}
    }
    @GameTest(template="wand_test_empty") public static void tinkererBedrockEntryProtectionAndStorageSafety(GameTestHelper test){
        var world=test.getLevel();var floor=test.absolutePos(new BlockPos(2,1,2));for(int y=0;y<=5;y++)world.setBlockAndUpdate(floor.above(y),Blocks.BEDROCK.defaultBlockState());var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTBedrockEntry"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void BedrockServerTests_place_1(net.minecraftforge.event.level.BlockEvent.EntityMultiPlaceEvent event){if(event.getEntity()==player)event.setCanceled(true);}};net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);try{test.assertTrue(!BedrockTravel.prepareEntry(world,player,floor),"Protected entry preparation rejected");for(int y=0;y<=5;y++)test.assertTrue(world.getBlockState(floor.above(y)).is(Blocks.BEDROCK),"All edits roll back together");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);}
        world.setBlockAndUpdate(floor.above(2),Blocks.CHEST.defaultBlockState());test.assertTrue(!BedrockTravel.prepareEntry(world,player,floor)&&world.getBlockState(floor.above(2)).is(Blocks.CHEST),"Entry cannot delete storage");world.setBlockAndUpdate(floor.above(2),Blocks.BEDROCK.defaultBlockState());test.assertTrue(BedrockTravel.prepareEntry(world,player,floor),"Normal bedrock shaft can be prepared");for(int y=1;y<=4;y++)test.assertTrue(world.isEmptyBlock(floor.above(y)),"Four-block entry clearance");test.assertTrue(world.getBlockState(floor.above(5)).is(Tinkerer.BEDROCK_PORTAL.get()),"Return portal is created above the chamber");for(int y=0;y<=5;y++)world.removeBlock(floor.above(y),false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererBedrockToolReplacementHonorsBreakProtection(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));world.setBlockAndUpdate(pos,Blocks.BEDROCK.defaultBlockState());var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTBedrockMining"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void BedrockServerTests_breaking_2(net.minecraftforge.event.level.BlockEvent.BreakEvent event){if(event.getPlayer()==player)event.setCanceled(true);}};net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);try{test.assertTrue(!BedrockTravel.replace(player,pos,Blocks.AIR.defaultBlockState(),true)&&world.getBlockState(pos).is(Blocks.BEDROCK),"Protected bedrock cannot be bypassed by the custom pick path");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);}
        test.assertTrue(BedrockTravel.replace(player,pos,Tinkerer.BEDROCK_PORTAL.get().defaultBlockState(),true),"Unprotected bedrock transforms to portal");test.assertTrue(BedrockTravel.replace(player,pos,Blocks.BEDROCK.defaultBlockState(),false),"Portal can be closed back into bedrock");world.removeBlock(pos,false);test.succeed();
    }
}
