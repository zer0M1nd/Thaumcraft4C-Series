package tc4revival.research;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
import tc4revival.network.*;

/** Clients report a hovered slot only. Every reward uses the current server menu and stack. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class InventoryScanning {
    public static final int SELF=-1, CANCEL=-2, DURATION=25;
    private static final Map<ServerPlayer,Use> USES=new IdentityHashMap<>();
    private static class Use {
        final AbstractContainerMenu menu; final int slot; final ItemStack tool,original;
        final ServerScanSession session; long seen; boolean finished;
        Use(ServerPlayer player,int slot) {
            this.menu=player.containerMenu;this.slot=slot;tool=menu.getCarried();
            original=slot<0?ItemStack.EMPTY:menu.getSlot(slot).getItem().copy();seen=player.server.getTickCount();
            session=new ServerScanSession(player,()->resolve(player,this),DURATION);
        }
    }
    public static boolean eligible(ServerPlayer p,int menuId,int slot) {
        if(!tc4revival.config.TCConfig.SERVER.revival.inventoryScanning.get())return false;
        if(p==null||!p.isAlive()||p.isSpectator()||p.isRemoved())return false;
        var menu=p.containerMenu;
        if(menu.containerId!=menuId||!menu.stillValid(p)||!menu.getCarried().is(TCItems.THAUMOMETER.get()))return false;
        if(slot==SELF)return menu==p.inventoryMenu;
        if(slot<0||slot>=menu.slots.size())return false;
        var target=menu.getSlot(slot);
        return target.isActive()&&target.mayPickup(p)&&target.hasItem()&&!(target instanceof ResultSlot)
                &&!(menu instanceof ArcaneWorkbenchMenu&&slot==0);
    }
    private static Optional<ScanTarget> resolve(ServerPlayer player,Use use) {
        if(player.containerMenu!=use.menu||use.menu.getCarried()!=use.tool
                ||!eligible(player,use.menu.containerId,use.slot))return Optional.empty();
        if(use.slot==SELF)return ScanTargetResolver.entity(player);
        var current=use.menu.getSlot(use.slot).getItem();
        if(!ItemStack.isSameItemSameTags(current,use.original))return Optional.empty();
        return ScanTargetResolver.inventory(current);
    }
    public static void request(ServerPlayer player,int menuId,int slot) {
        if(player==null)return;
        if(slot==CANCEL){cancel(player);return;}
        if(!eligible(player,menuId,slot)){cancel(player);status(player,menuId,slot,false);return;}
        var use=USES.get(player);
        if(use!=null&&use.menu==player.containerMenu&&use.slot==slot&&resolve(player,use).isPresent()) {
            use.seen=player.server.getTickCount();return;
        }
        cancel(player);use=new Use(player,slot);
        boolean active=use.session.begin()==ScanProgress.State.TRACKING;
        if(active)USES.put(player,use);
        status(player,menuId,slot,active);
    }
    private static void status(ServerPlayer p,int menu,int slot,boolean active) { TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new InventoryScanStatus(menu,slot,active)); }
    public static void cancel(ServerPlayer player){var use=USES.remove(player);if(use!=null)use.session.cancel();}
    public static void tick(ServerPlayer player) {
        var use=USES.get(player);if(use==null)return;
        if(player.server.getTickCount()-use.seen>10||resolve(player,use).isEmpty()){cancel(player);return;}
        if(use.finished)return;
        var result=use.session.tick();
        result.result().ifPresent(value->TCNetwork.send(player,ScanFeedbackPacket.from(value)));
        result.clue().ifPresent(value->TCNetwork.send(player,new CluePacket(value)));
        if(result.state()!=ScanProgress.State.TRACKING){use.finished=true;status(player,use.menu.containerId,use.slot,false);}
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p)tick(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){USES.clear();}
}
