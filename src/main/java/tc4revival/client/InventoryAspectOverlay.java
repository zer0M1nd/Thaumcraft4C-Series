package tc4revival.client;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.config.TCConfig;
import tc4revival.network.*;
import tc4revival.research.InventoryAspectDisplay;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class InventoryAspectOverlay {
    private static Screen screen;
    private static ItemStack hovered=ItemStack.EMPTY;
    private static List<InventoryAspectDisplay.Icon> icons=List.of();
    private static boolean self;
    private static int request;
    private static long nextQuery;
    private static int dataVersion=-1;
    private static Connection connection;
    private static ResourceLocation texture(String name) { return name.startsWith("_")?ResourceLocation.parse("thaumcraft:textures/aspects/"+name+".png"):thaumcraft.api.client.ClientAspectViews.texture(name); }
    private static void clear() { screen=null;self=false;hovered=ItemStack.EMPTY;icons=List.of();nextQuery=0;request++;connection=null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void opened(ScreenEvent.Opening event) { clear(); }
    public static void receive(InventoryAspectsReply packet,Connection source) {
        if(source!=connection || packet.request()!=request || Minecraft.getInstance().screen!=screen)return;
        icons=packet.icons();
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        var mc=Minecraft.getInstance();
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> container) || mc.player==null || mc.getConnection()==null || mc.mouseHandler.isMouseGrabbed())return;
        if(!InventoryScanHover.holdingScanner()&&!InventoryAspectDisplay.visible(TCConfig.CLIENT.displayAspects.get(),Screen.hasShiftDown()))return;
        var slot=container.getSlotUnderMouse();var stack=slot==null?ItemStack.EMPTY:slot.getItem();
        boolean onSelf=stack.isEmpty()&&container.getMenu().getCarried().is(tc4revival.content.TCItems.THAUMOMETER.get())&&InventoryScanHover.hoveringSelf(container,event.getMouseX(),event.getMouseY());
        if(stack.isEmpty()&&!onSelf){if(!hovered.isEmpty()||self)clear();return;}
        if(screen!=container || self!=onSelf || !ItemStack.isSameItemSameTags(hovered,stack)) {
            self=onSelf;
            screen=container;hovered=stack.copyWithCount(1);icons=List.of();request++;nextQuery=0;
        }
        long now=mc.level.getGameTime();
        if(dataVersion!=ClientItemAspects.version()){dataVersion=ClientItemAspects.version();icons=List.of();nextQuery=0;}
        if(now>=nextQuery) {
            connection=mc.getConnection().getConnection();nextQuery=now+10;
            if(onSelf)TCNetwork.CHANNEL.sendToServer(new InventoryAspectsRequest(request,ItemStack.EMPTY));
            else icons=ClientItemAspects.resolve(hovered).map(value->InventoryAspectDisplay.present(value.identity(),value.amounts(),ClientKnowledge.displaySnapshot().orElse(null))).orElse(List.of());
        }
        var g=event.getGuiGraphics();g.pose().pushPose();g.pose().translate(0,0,500);com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        try {
            for(int i=0;i<icons.size();i++) {
                var icon=icons.get(i);int x=event.getMouseX()+9+i*18,y=event.getMouseY()-34;
                g.setColor(1,1,1,1);LegacyGui.blit(g,texture("_back"),x-2,y-2,0,0,20,20,20,20);
                var aspect=tc4revival.aspect.TCAspects.catalog().find(icon.tag()).orElse(null);
                if(aspect==null)LegacyGui.blit(g,texture(icon.tag().equals(tc4revival.research.ScanIdentity.EMPTY_ICON)?"_empty":"_unknown"),x,y,0,0,16,16,16,16);
                else {
                    int color=aspect.color();g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
                    LegacyGui.blit(g,texture(aspect.tag()),x,y,0,0,16,16,16,16);g.setColor(1,1,1,1);
                    if(icon.amount()>0) {
                        g.pose().pushPose();g.pose().scale(.5f,.5f,1);String amount=Integer.toString(icon.amount());int tx=x*2+32-mc.font.width(amount),ty=y*2+32-mc.font.lineHeight;
                        for(int[] delta:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})g.drawString(mc.font,amount,tx+delta[0],ty+delta[1],0xff000000,false);
                        g.drawString(mc.font,amount,tx,ty,0xffffffff,false);g.pose().popPose();
                    }
                }
            }
        } finally { g.setColor(1,1,1,1);g.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.disableBlend(); }
    }
    private InventoryAspectOverlay() {}
}
