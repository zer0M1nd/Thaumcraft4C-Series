package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
import tc4revival.network.*;
import tc4revival.research.*;

/** Legacy 1.0.11 cursor scanning, with server-owned validation and rewards. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class InventoryScanHover {
    private static int previous=-2,menu=-1,elapsed;
    private static boolean tracking;
    private static ItemStack stack=ItemStack.EMPTY;
    private static void clear(){previous=-2;menu=-1;elapsed=0;tracking=false;stack=ItemStack.EMPTY;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    public static boolean holdingScanner() {
        var p=Minecraft.getInstance().player;
        return p!=null&&(p.containerMenu.getCarried().is(TCItems.THAUMOMETER.get())||p.getMainHandItem().is(TCItems.THAUMOMETER.get())||p.getOffhandItem().is(TCItems.THAUMOMETER.get()));
    }
    public static boolean hoveringSelf(AbstractContainerScreen<?> screen,double x,double y) {
        return screen instanceof InventoryScreen&&x>=screen.getGuiLeft()+26&&x<screen.getGuiLeft()+78&&y>=screen.getGuiTop()+8&&y<screen.getGuiTop()+78;
    }
    public static void status(InventoryScanStatus packet,net.minecraft.network.Connection source) {
        var mc=Minecraft.getInstance();
        if(mc.getConnection()==null||source!=mc.getConnection().getConnection()||packet.menu()!=menu||packet.slot()!=previous)return;
        tracking=packet.tracking();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        if(!tc4revival.config.TCConfig.SERVER.revival.inventoryScanning.get()){clear();return;}
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getConnection()==null){clear();return;}
        int target=-2,id=mc.player.containerMenu.containerId;ItemStack current=ItemStack.EMPTY;
        if(mc.screen instanceof AbstractContainerScreen<?> screen&&screen.getMenu().getCarried().is(TCItems.THAUMOMETER.get())) {
            var slot=screen.getSlotUnderMouse();
            if(slot!=null&&slot.hasItem()&&slot.isActive()&&slot.mayPickup(mc.player)&&!(slot instanceof net.minecraft.world.inventory.ResultSlot)) {
                target=slot.index;current=slot.getItem();
            } else {
                double x=mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth();
                double y=mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight();
                if(hoveringSelf(screen,x,y))target=-1;
            }
        }
        boolean changed=target!=previous||id!=menu||!ItemStack.isSameItemSameTags(stack,current);
        if(changed){elapsed=0;stack=current.copy();tracking=false;}
        if(changed||(target!=-2&&elapsed%5==0))TCNetwork.CHANNEL.sendToServer(new InventoryScanPacket(id,target));
        previous=target;menu=id;
        if(target!=-2) {
            elapsed++;
            // The legacy handler runs in both START and END phases: 50 callbacks = 25 game ticks.
            if(tracking&&elapsed>2&&elapsed<=InventoryScanning.DURATION)
                mc.player.playSound(TCSounds.CAMERA_TICKS.get(),.2f,.45f+mc.player.getRandom().nextFloat()*.1f);
        }
    }
    @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e) {
        if(!e.getItemStack().is(TCItems.THAUMOMETER.get())||!tc4revival.config.TCConfig.SERVER.revival.inventoryScanning.get())return;
        e.getToolTip().add(Component.translatable("thaumcraft.inventory_scan.hint").withStyle(net.minecraft.ChatFormatting.GOLD));
        if(Screen.hasShiftDown())e.getToolTip().add(Component.translatable("thaumcraft.inventory_scan.help").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post e){
        if(!tc4revival.config.TCConfig.SERVER.revival.inventoryScanning.get())return;
        var mc=Minecraft.getInstance();
        if(!(e.getScreen() instanceof AbstractContainerScreen<?> screen)||mc.player==null||!screen.getMenu().getCarried().is(TCItems.THAUMOMETER.get()))return;
        // Keep the carried-lens tooltip at the same depth as ordinary item tooltips.
        // The aspect overlay deliberately sits above that shared tooltip plane.
        var g=e.getGuiGraphics();g.pose().pushPose();
        try {
            if(tracking&&elapsed<=InventoryScanning.DURATION) {
                int dots=Math.min(3,elapsed*4/InventoryScanning.DURATION);
                g.drawString(mc.font,Component.translatable("thaumcraft.inventory_scan.scanning").append(".".repeat(dots)),e.getMouseX(),e.getMouseY()-30,0xFFAA00,true);
            }
            var slot=screen.getSlotUnderMouse();
            if(slot!=null&&slot.hasItem())g.renderTooltip(mc.font,slot.getItem(),e.getMouseX(),e.getMouseY());
        } finally {g.pose().popPose();}
    }
}
