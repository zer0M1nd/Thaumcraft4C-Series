package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingAuditServerTests {
    private static ItemStack wand(FakePlayer player) {
        var stack=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(stack,new ItemStack(TCItems.WARDING_FOCUS.get()));player.setItemInHand(InteractionHand.MAIN_HAND,stack);return stack;
    }
    private static void cast(FakePlayer player,ItemStack wand,BlockPos pos) {
        // Isolate transactions from wall-clock debounce without constructing items after registry freeze.
        var focus=TCItems.WARDING_FOCUS.get();
        try {
            var field=WardingFocusItem.class.getDeclaredField("delays");field.setAccessible(true);
            @SuppressWarnings("unchecked") var delays=(Map<net.minecraft.server.level.ServerLevel,Map<BlockPos,Long>>)field.get(focus);
            var positions=delays.get(player.serverLevel());if(positions!=null)positions.remove(pos);
        } catch (ReflectiveOperationException failure) {throw new AssertionError(failure);}
        focus.cast(wand,player.level(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    @GameTest(template="wand_test_empty")
    public static void wardingTransactionsPreserveStateOwnerLightAndRestoreForFree(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,15,4));var previous=level.getBlockState(pos);
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardOwner"));var stranger=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardStranger"));
        var states=List.of(Blocks.STONE.defaultBlockState(),Blocks.IRON_BLOCK.defaultBlockState(),Blocks.REDSTONE_BLOCK.defaultBlockState(),Blocks.RED_WOOL.defaultBlockState(),Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X),Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.Z),Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT,true),Blocks.BEDROCK.defaultBlockState());
        try {
            for(var original:states) {
                level.setBlock(pos,original,2);var stack=wand(owner);var before=stack.copy();cast(owner,stack,pos);
                test.assertTrue(level.getBlockEntity(pos) instanceof WardedBlockEntity,"Eligible block protected: "+original);var ward=(WardedBlockEntity)level.getBlockEntity(pos);
                test.assertTrue(ward.original().equals(original)&&ward.ownedBy(owner)&&!ward.ownedBy(stranger),"Original properties and owner retained");
                test.assertTrue(level.getBlockState(pos).getValue(WardedBlock.LIGHT)==original.getLightEmission(level,pos),"Light value carried into proxy state");
                for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){int expected=switch(aspect.tag()){case "terra","ordo"->25;case "aqua"->10;default->0;};test.assertTrue(TCItems.CASTING_WAND.get().vis(before,aspect)-TCItems.CASTING_WAND.get().vis(stack,aspect)==expected,"Exact per-block payment");}
                var copy=new WardedBlockEntity(pos,ward.getBlockState());copy.load(ward.saveWithoutMetadata());test.assertTrue(copy.original().equals(original)&&copy.ownedBy(owner),"Fresh block entity NBT retains state and UUID");
                var other=wand(stranger);var otherBefore=other.copy();cast(stranger,other,pos);test.assertTrue(level.getBlockEntity(pos)==ward&&ItemStack.matches(other,otherBefore),"Foreign player cannot restore or pay for someone else's ward");
                var paid=stack.copy();cast(owner,stack,pos);test.assertTrue(level.getBlockState(pos).equals(original)&&level.getBlockEntity(pos)==null,"Owner restores exact source block properties");test.assertTrue(ItemStack.matches(stack,paid),"Restoration is free");
            }
            tc4revival.TC4Revival.LOGGER.info("PASS wardingTransactionsPreserveStateOwnerLightAndRestoreForFree: eight original states, ownership/NBT/light/payment/free restoration");test.succeed();
        } finally {level.setBlock(pos,previous,2);}
    }
    @GameTest(template="wand_test_empty")
    public static void wardingArchitectRotatesMixedVolumesAndStopsAtInsufficientVis(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(4,20,4));var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardArea"));
        var snapshot=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        BlockPos.betweenClosed(origin.offset(-3,-3,-3),origin.offset(3,3,3)).forEach(p->snapshot.put(p.immutable(),level.getBlockState(p)));
        var focus=TCItems.WARDING_FOCUS.get();
        try {
            for(var face:Direction.values()) {
                snapshot.keySet().forEach(p->level.setBlock(p,((p.getX()+p.getY()+p.getZ())&1)==0?Blocks.STONE.defaultBlockState():Blocks.IRON_BLOCK.defaultBlockState(),2));
                var stack=wand(player);var core=new ItemStack(focus);focus.applyUpgrade(core,FocusUpgrade.ARCHITECT,2);WandFocusItem.attach(stack,core);
                stack.getOrCreateTag().putInt("areax",1);stack.getOrCreateTag().putInt("areay",2);stack.getOrCreateTag().putInt("areaz",0);
                var selected=focus.blocks(stack,level,origin,face,player);
                test.assertTrue(selected.size()==15,"Asymmetric connected volume contains 15 mixed-material blocks: "+face);
                for(var pos:selected) {
                    var d=pos.subtract(origin);
                    int a=face.getAxis()==Direction.Axis.Y?d.getX():d.getY();
                    int b=face.getAxis().choose(d.getX(),d.getY(),d.getZ());
                    int c=face.getAxis()==Direction.Axis.Z?d.getX():d.getZ();
                    test.assertTrue(Math.abs(a)<=1&&Math.abs(b)<=2&&c==0,"Legacy face-oriented axes: "+face+" "+d);
                }
                // Exactly two blocks affordable; third must not receive a partial debit.
                stack.getOrCreateTag().putInt("terra",50);stack.getOrCreateTag().putInt("ordo",50);stack.getOrCreateTag().putInt("aqua",20);
                // This transaction uses UP; face rotation was checked through the real selection entry above.
                var paidOrder=focus.blocks(stack,level,origin,Direction.UP,player);cast(player,stack,origin);
                for(int i=0;i<paidOrder.size();i++)test.assertTrue((level.getBlockEntity(paidOrder.get(i)) instanceof WardedBlockEntity)==(i<2),"Only the first two DFS blocks are protected");
                test.assertTrue(stack.getTag().getInt("terra")==0&&stack.getTag().getInt("ordo")==0&&stack.getTag().getInt("aqua")==0,"Two complete payments and no partial third debit");
                cast(player,stack,origin);
                for(var p:paidOrder)test.assertTrue(!(level.getBlockEntity(p) instanceof WardedBlockEntity),"Connected wards restore with empty wand");
            }
            tc4revival.TC4Revival.LOGGER.info("PASS wardingArchitectRotatesMixedVolumesAndStopsAtInsufficientVis: six face selections, mixed materials, ordered two-block payment and empty-wand restoration");test.succeed();
        } finally {snapshot.forEach((p,state)->level.setBlock(p,state,2));}
    }
    @GameTest(template="wand_test_empty")
    public static void wardingRejectsIneligibleBlocksAndCancelledProtectionWithoutPayment(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,15,4));var previous=level.getBlockState(pos);var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardReject"));
        Consumer<BlockEvent.BreakEvent> reject=e->{if(e.getLevel()==level&&e.getPos().equals(pos))e.setCanceled(true);};
        try {
            for(var block:List.of(Blocks.AIR,Blocks.GLASS,Blocks.CHEST,Blocks.OAK_SLAB)) {
                level.setBlock(pos,block.defaultBlockState(),2);var state=level.getBlockState(pos);var stack=wand(player);var before=stack.copy();cast(player,stack,pos);
                test.assertTrue(level.getBlockState(pos).equals(state)&&ItemStack.matches(stack,before),"Ineligible block remains unchanged without charge: "+block);
            }
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);MinecraftForge.EVENT_BUS.addListener(reject);
            var stack=wand(player);var before=stack.copy();cast(player,stack,pos);
            test.assertTrue(level.getBlockState(pos).is(Blocks.STONE)&&ItemStack.matches(stack,before),"Forge protection cancellation leaves source and vis intact");
            tc4revival.TC4Revival.LOGGER.info("PASS wardingRejectsIneligibleBlocksAndCancelledProtectionWithoutPayment: air/glass/chest/slab and Forge cancellation");test.succeed();
        } finally {MinecraftForge.EVENT_BUS.unregister(reject);level.setBlock(pos,previous,2);}
    }
}
