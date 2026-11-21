package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VisInteractionServerTests {
    @GameTest(template="wand_test_empty")
    public static void relayAndChargerShardAndWandInteractions(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RelayInteraction"));player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);int cases=0;
        try {
            level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
            for(var block:new VisRelayBlock[]{TCArcane.VIS_RELAY.get(),TCArcane.WORKBENCH_CHARGER.get()}){
                level.setBlock(pos,block.defaultBlockState(),2);var relay=(VisRelayBlockEntity)level.getBlockEntity(pos);
                for(var hand:InteractionHand.values())for(var shard:ShardType.values()){
                    relay.setAttunement(-1);player.setItemInHand(hand,new ItemStack(TCItems.shard(shard),3));var held=player.getItemInHand(hand).copy();
                    test.assertTrue(level.getBlockState(pos).use(level,player,hand,hit).consumesAction()&&relay.attunement()==(shard.metadata()<6?shard.metadata():-1),"Shard chooses primal color or clears with balanced shard");
                    level.getBlockState(pos).use(level,player,hand,hit);
                    test.assertTrue(relay.attunement()==-1&&ItemStack.matches(held,player.getItemInHand(hand)),"Repeated same shard clears color without consuming shard");
                    player.setShiftKeyDown(true);test.assertTrue(level.getBlockState(pos).use(level,player,hand,hit)==InteractionResult.PASS&&relay.attunement()==-1,"Sneaking bypasses shard tuning");player.setShiftKeyDown(false);cases++;
                }
                var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();
                for(int step=0;step<7;step++){
                    relay.onWandUseFirst(wand,new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                    test.assertTrue(relay.attunement()==(step<6?step:-1)&&ItemStack.matches(wand,before),"Wand cycles six colors then uncolored without spending vis");
                }
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));test.assertTrue(level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"Unrelated item does not tune relay");
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            tc4revival.TC4Revival.LOGGER.info("PASS relayAndChargerShardAndWandInteractions: {} block/hand/shard cases, repeat and sneak plus two seven-step wand cycles",cases);test.succeed();
        }finally{level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);}
    }
    @GameTest(template="wand_test_empty")
    public static void visSavedEntitiesRebuildWithoutOldReferences(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var aPos=pos.east(6);var bPos=pos.east(12);var air=TCAspects.catalog().require("aer");
        // The chain extends beyond the eight-block template, including a second chunk.
        // Clear the actual beam corridor; unmodified flat-world soil otherwise blocks it.
        level.getChunkAt(bPos.east(2));
        for(var point:BlockPos.betweenClosed(pos,pos.east(14))) level.setBlock(point,Blocks.AIR.defaultBlockState(),2);
        level.setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);
        for(var target:new BlockPos[]{aPos,bPos}){level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(target,TCArcane.VIS_RELAY.get().defaultBlockState(),2);}
        var source=(EnergizedNodeBlockEntity)level.getBlockEntity(pos);source.configure(new AspectAmounts().add(air,100),AuraNodeType.NORMAL,null,"saved-source");source.tick();
        var a=(VisRelayBlockEntity)level.getBlockEntity(aPos);var b=(VisRelayBlockEntity)level.getBlockEntity(bPos);a.setAttunement(2);b.setAttunement(2);source.tickNetwork();a.tickNetwork();b.tickNetwork();
        try {
            test.assertTrue(b.root()==source&&VisNetwork.drain(level,bPos.east(2),air,4)==4,"Original saved chain supplies consumer");
            var sourceTag=source.saveWithoutMetadata();var aTag=a.saveWithoutMetadata();var bTag=b.saveWithoutMetadata();
            level.removeBlockEntity(pos);level.removeBlockEntity(aPos);level.removeBlockEntity(bPos);
            test.assertTrue(VisNetwork.drain(level,bPos.east(2),air,1)==0,"Removed entity instances cannot continue supplying");
            var restoredSource=new EnergizedNodeBlockEntity(pos,level.getBlockState(pos));restoredSource.load(sourceTag);level.setBlockEntity(restoredSource);
            var restoredA=new VisRelayBlockEntity(aPos,level.getBlockState(aPos));restoredA.load(aTag);level.setBlockEntity(restoredA);
            var restoredB=new VisRelayBlockEntity(bPos,level.getBlockState(bPos));restoredB.load(bTag);level.setBlockEntity(restoredB);
            test.assertTrue(restoredB.parent()==null,"Deserialized entities have no stale parent reference");
            restoredSource.tick();restoredA.tickNetwork();restoredB.tickNetwork();
            test.assertTrue(restoredA.parent()==restoredSource&&restoredB.parent()==restoredA&&restoredB.attunement()==2,"Saved color and fresh entity instances rebuild topology");
            test.assertTrue(VisNetwork.drain(level,bPos.east(2),air,10)==10&&source.consumeVis(air,1)==0,"Rebuilt source supplies own budget while old instance remains invalid");
            tc4revival.TC4Revival.LOGGER.info("PASS visSavedEntitiesRebuildWithoutOldReferences: entity NBT, removed references, fresh source and two-relay reconstruction");test.succeed();
        }finally{for(var target:new BlockPos[]{pos,aPos,bPos})level.setBlock(target,Blocks.AIR.defaultBlockState(),2);for(var target:new BlockPos[]{aPos,bPos})level.setBlock(target.below(),Blocks.AIR.defaultBlockState(),2);}
    }
}
