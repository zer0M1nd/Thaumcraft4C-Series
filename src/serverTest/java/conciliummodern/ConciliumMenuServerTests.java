package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.TCItems;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumMenuServerTests {
    private static Player player(GameTestHelper test){
        var player=new Player(test.getLevel(),test.absolutePos(new BlockPos(3,3,3)),0,new GameProfile(UUID.randomUUID(),"ConciliumMenu")){
            @Override public boolean isSpectator(){return false;}
            @Override public boolean isCreative(){return false;}
        };player.setPos(test.absoluteVec(new net.minecraft.world.phys.Vec3(3.5,3,3.5)));return player;
    }
    private static net.minecraft.server.level.ServerPlayer serverPlayer(GameTestHelper test){
        var player=new net.minecraft.server.level.ServerPlayer(test.getLevel().getServer(),test.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumTrade"));
        new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},player);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(test.absoluteVec(new net.minecraft.world.phys.Vec3(3.5,3,3.5)));test.getLevel().addNewPlayer(player);return player;
    }
    private static ItemStack namedWand(){var wand=new ItemStack(TCItems.CASTING_WAND.get());wand.getOrCreateTag().putString("Xylography","TestTarget");wand.getTag().putString("unchanged","vis and upgrades");return wand;}
    @GameTest(template="empty") public static void monitorMenuWritesThroughAndLocksItsExactCarrier(GameTestHelper test){
        var player=player(test);var monitor=new ItemStack(ConciliumItems.ASTRAL_MONITOR.get());player.getInventory().setItem(0,monitor);player.getInventory().setItem(9,namedWand());
        var menu=new AstralMonitorMenu(1,player.getInventory(),0);player.containerMenu=menu;
        test.assertTrue(menu.stillValid(player),"exact carrier opens menu");test.assertTrue(!menu.quickMoveStack(player,1).isEmpty(),"shift-click inserts named wand");
        test.assertTrue(player.getInventory().getItem(9).isEmpty()&&AstralMonitor.stored(monitor).getTag().getString("unchanged").equals("vis and upgrades"),"stored data written before close");
        test.assertTrue(menu.quickMoveStack(player,28).isEmpty(),"cannot shift the carrier");menu.clicked(0,0,ClickType.SWAP,player);
        test.assertTrue(player.getInventory().getItem(0)==monitor&&menu.getSlot(0).hasItem(),"hotbar swap cannot steal carrier");
        player.getInventory().setItem(0,monitor.copy());test.assertTrue(!menu.stillValid(player),"replacement copy invalidates stale menu");
        test.assertTrue(menu.quickMoveStack(player,0).isEmpty(),"stale menu cannot extract a duplicate");test.succeed();
    }
    @GameTest(template="empty") public static void offhandMonitorRejectsOffhandSwapAndReturnsStoredWandOnce(GameTestHelper test){
        var player=player(test);var monitor=new ItemStack(ConciliumItems.ASTRAL_MONITOR.get());monitor.getOrCreateTag().put("wand",namedWand().save(new CompoundTag()));player.getInventory().setItem(40,monitor);
        var menu=new AstralMonitorMenu(2,player.getInventory(),40);player.containerMenu=menu;menu.clicked(0,40,ClickType.SWAP,player);
        test.assertTrue(player.getOffhandItem()==monitor&&menu.getSlot(0).hasItem(),"offhand swap blocked");
        test.assertTrue(!menu.quickMoveStack(player,0).isEmpty()&&AstralMonitor.stored(monitor).isEmpty(),"extract writes empty carrier immediately");
        test.assertTrue(menu.quickMoveStack(player,0).isEmpty(),"second extract empty");
        int wands=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(TCItems.CASTING_WAND.get()))wands+=player.getInventory().getItem(i).getCount();
        test.assertTrue(wands==1,"only one stored wand returned");test.succeed();
    }
    @GameTest(template="empty") public static void tradeButtonRequiresCurrentCustomerAndReturnsUnspentOffers(GameTestHelper test){
        // Vanilla clearContainer returns inventory contents only to an actual ServerPlayer.
        var player=serverPlayer(test);var stranger=serverPlayer(test);var merchant=ConciliumEntities.THAUMATURGE.get().create(test.getLevel());merchant.setPos(player.position());merchant.setNoAi(true);test.getLevel().addFreshEntity(merchant);merchant.customer=player;
        try{
        var menu=new ThaumaturgeMenu(3,player.getInventory(),merchant);player.containerMenu=menu;menu.getSlot(0).set(new ItemStack(Items.EXPERIENCE_BOTTLE,3));
        test.assertTrue(!menu.clickMenuButton(player,999)&&menu.getSlot(0).getItem().getCount()==3,"invalid button cannot consume offer");
        stranger.containerMenu=menu;test.assertTrue(!menu.clickMenuButton(stranger,0),"other player cannot trade");
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND));test.assertTrue(!menu.clickMenuButton(player,0)&&menu.getSlot(0).getItem().getCount()==3,"occupied reward blocks trade without consuming offer");
        menu.removed(player);test.assertTrue(merchant.customer==null,"closing clears merchant customer");
        int offers=0,diamonds=0;for(int i=0;i<player.getInventory().getContainerSize();i++){var item=player.getInventory().getItem(i);if(item.is(Items.EXPERIENCE_BOTTLE))offers+=item.getCount();if(item.is(Items.DIAMOND))diamonds+=item.getCount();}
        test.assertTrue(offers==3&&diamonds==1,"close returns both offer and unclaimed reward");
        menu.removed(player);int again=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.DIAMOND))again+=player.getInventory().getItem(i).getCount();
        test.assertTrue(again==1,"repeat close cannot duplicate reward");test.succeed();
        }finally{player.containerMenu=player.inventoryMenu;stranger.containerMenu=stranger.inventoryMenu;merchant.discard();stranger.discard();player.discard();}
    }
}
