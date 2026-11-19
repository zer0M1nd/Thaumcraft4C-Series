package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class BlockSwapServerTests {
    @GameTest(template="wand_test_empty")
    public static void swapQueueHonorsInteractionCancellation(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SwapQueueTest"));player.setGameMode(GameType.SURVIVAL);
        player.setPos(pos.getX(),pos.getY()+2,pos.getZ());
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);
        WandFocusItem.attach(wand,new ItemStack(TCItems.TRADE_FOCUS.get()));
        player.getInventory().setItem(40,wand);player.getInventory().setItem(9,new ItemStack(Items.DIRT,4));
        var states=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        int[] calls={0};
        java.util.function.Consumer<PlayerInteractEvent.RightClickBlock> guard=event->{
            if(event.getEntity()!=player)return;
            calls[0]++;
            test.assertTrue(event.getHand()==InteractionHand.OFF_HAND && event.getFace()==Direction.UP,"Queued interaction preserves wand hand and old top face");
            if(event.getPos().equals(pos))event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false,PlayerInteractEvent.RightClickBlock.class,guard);
        try {
            for(int x=0;x<3;x++) {var p=pos.east(x);states.put(p,level.getBlockState(p));level.setBlock(p,Blocks.STONE.defaultBlockState(),3);}
            var before=wand.copy();
            BlockSwaps.enqueue(player,InteractionHand.OFF_HAND,pos,new ItemStack(Items.DIRT),0);
            BlockSwaps.process(level,id->id.equals(player.getUUID())?player:null);
            test.assertTrue(calls[0]==1 && level.getBlockState(pos).is(Blocks.STONE),"Actual queue posts a cancellable interaction before replacement");
            test.assertTrue(ItemStack.matches(wand,before) && player.getInventory().getItem(9).getCount()==4 && player.getInventory().countItem(Items.COBBLESTONE)==0,"Canceled entry neither charges vis nor consumes material nor returns old drops");
            for(int x=0;x<3;x++)BlockSwaps.enqueue(player,InteractionHand.OFF_HAND,pos.east(x),new ItemStack(Items.DIRT),0);
            BlockSwaps.process(level,id->player);
            test.assertTrue(calls[0]==3 && level.getBlockState(pos.east()).is(Blocks.DIRT) && level.getBlockState(pos.east(2)).is(Blocks.STONE),"Canceled work is skipped but only one successful replacement runs per tick");
            test.assertTrue(player.getInventory().getItem(9).getCount()==3 && player.getInventory().countItem(Items.COBBLESTONE)==1 && !ItemStack.matches(wand,before),"Successful exchange pays once and returns exactly one old drop");
            BlockSwaps.process(level,id->player);
            test.assertTrue(calls[0]==4 && level.getBlockState(pos.east(2)).is(Blocks.DIRT) && player.getInventory().getItem(9).getCount()==2,"Remaining work resumes on the next queue tick");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(guard);
            BlockSwaps.process(level,id->null);
            states.forEach((p,state)->level.setBlock(p,state,3));
        }
        tc4revival.TC4Revival.LOGGER.info("Block swap PASS: interaction-cancel-no-payment-offhand-skip-one-success-next-tick");test.succeed();
    }
}
