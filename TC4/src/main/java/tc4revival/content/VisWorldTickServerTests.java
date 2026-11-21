package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VisWorldTickServerTests {
    @GameTest(template="wand_test_empty")
    public static void relayItemPlacesOnSixFacesAndBreaksWithoutSupport(GameTestHelper test){
        var level=test.getLevel();var support=test.absolutePos(new BlockPos(3,3,3));
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RelayPlace"));player.setGameMode(GameType.SURVIVAL);player.moveTo(support.getX()+3,support.getY()+2,support.getZ()+3,0,0);
        try {
            for(var face:Direction.values()){
                var target=support.relative(face);level.setBlock(target,Blocks.AIR.defaultBlockState(),2);level.setBlock(support,Blocks.STONE.defaultBlockState(),2);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCArcane.VIS_RELAY.get(),2));
                var hit=new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,support,false);
                var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                test.assertTrue(result.consumesAction()&&level.getBlockState(target).is(TCArcane.VIS_RELAY.get())&&level.getBlockState(target).getValue(VisRelayBlock.FACING)==face,"Actual BlockItem places outward on "+face);
                test.assertTrue(player.getMainHandItem().getCount()==1&&level.getBlockEntity(target) instanceof VisRelayBlockEntity,"One item consumed and relay BE created");
                level.setBlock(support,Blocks.AIR.defaultBlockState(),3);
                test.assertTrue(level.getBlockState(target).isAir(),"Neighbor update removes unsupported relay on "+face);
            }
            tc4revival.TC4Revival.LOGGER.info("PASS relayItemPlacesOnSixFacesAndBreaksWithoutSupport: six actual item placements, orientation, item debit and support removal");test.succeed();
        }finally{for(var face:Direction.values())level.setBlock(support.relative(face),Blocks.AIR.defaultBlockState(),2);level.setBlock(support,Blocks.AIR.defaultBlockState(),2);}
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void chargerNaturallyTicksAndStopsAfterSourceRemoval(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var nodePos=pos.east(4);int cx=pos.getX()>>4,cz=pos.getZ()>>4;boolean added=level.setChunkForced(cx,cz,true);
        level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),2);var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        level.setBlock(nodePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);var node=(EnergizedNodeBlockEntity)level.getBlockEntity(nodePos);var aura=new AspectAmounts();for(var aspect:TCAspects.catalog().primals())aura.add(aspect,100);node.configure(aura,AuraNodeType.NORMAL,null,"natural-charge");
        level.setBlock(pos.above(),TCArcane.WORKBENCH_CHARGER.get().defaultBlockState(),2);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);int capacity=TCItems.CASTING_WAND.get().capacity(wand);for(var aspect:TCAspects.catalog().primals())wand.getOrCreateTag().putInt(aspect.tag(),capacity-40);table.setItem(10,wand);
        Runnable clear=()->{table.clearContent();level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(nodePos,Blocks.AIR.defaultBlockState(),2);if(added)level.setChunkForced(cx,cz,false);};
        // Allow one 40-tick parent-discovery interval regardless of initial ticker ordering.
        test.runAfterDelay(60,()->{
            try {
                for(var aspect:TCAspects.catalog().primals())test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==capacity,"Natural world ticker fills all six primals without manual callbacks");
                level.setBlock(nodePos,Blocks.AIR.defaultBlockState(),2);for(var aspect:TCAspects.catalog().primals())wand.getOrCreateTag().putInt(aspect.tag(),capacity-10);
            }catch(RuntimeException|AssertionError failure){clear.run();throw failure;}
        });
        test.runAfterDelay(75,()->{
            try {
                for(var aspect:TCAspects.catalog().primals())test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==capacity-10,"Natural ticks cannot draw from removed source");
                tc4revival.TC4Revival.LOGGER.info("PASS chargerNaturallyTicksAndStopsAfterSourceRemoval: world-only callbacks, six-primal fill and source-removal stop");test.succeed();
            }finally{clear.run();}
        });
    }
}
