package ttmodern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class TabletScreen extends AbstractContainerScreen<TabletMenu> {
    private Button left,right,redstone;
    public TabletScreen(TabletMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Tinkerer.TABLET_MENU.get(),TabletScreen::new));}
    @Override protected void init(){super.init();
        addRenderableWidget(new LegacyToggle(leftPos+52,topPos+15,"dynamism_tablet",Component.translatable("ttmisc.leftClick"),()->menu.entity.leftClick(),()->send(0)));
        addRenderableWidget(new LegacyToggle(leftPos+111,topPos+15,"dynamism_tablet",Component.translatable("ttmisc.rightClick"),()->!menu.entity.leftClick(),()->send(1)));
        addRenderableWidget(new LegacyToggle(leftPos+81,topPos+60,"dynamism_tablet",Component.translatable("ttmisc.redstoneControl"),()->menu.entity.redstone(),()->send(2)));
    }
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse(Tinkerer.ID+":textures/gui/dynamism_tablet.png"),leftPos,topPos,0,0,imageWidth,imageHeight);

    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){
        var left=Component.translatable("ttmisc.leftClick");var red=Component.translatable("ttmisc.redstoneControl");
        graphics.drawString(font,left,48-font.width(left),18,0x999999,false);graphics.drawString(font,Component.translatable("ttmisc.rightClick"),128,18,0x999999,false);graphics.drawString(font,red,88-font.width(red)/2,50,0x999999,false);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
