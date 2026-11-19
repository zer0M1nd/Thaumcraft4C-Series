package ttmodern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class MagnetScreen extends AbstractContainerScreen<MagnetMenu> {
    private Button adult,child;
    public MagnetScreen(MagnetMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Tinkerer.MAGNET_MENU.get(),MagnetScreen::new));}
    @Override protected void init(){super.init();adult=addRenderableWidget(new LegacyToggle(leftPos+100,topPos+28,"mob_magnet",Component.translatable("ttmisc.adult"),()->menu.entity.adult(),()->select(0)));child=addRenderableWidget(new LegacyToggle(leftPos+100,topPos+48,"mob_magnet",Component.translatable("ttmisc.child"),()->!menu.entity.adult(),()->select(1)));}
    private void select(int mode){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,mode);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse(Tinkerer.ID+":textures/gui/mob_magnet.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){
        graphics.drawString(font,Component.translatable("ttmisc.adult"),120,30,0x999999,false);graphics.drawString(font,Component.translatable("ttmisc.child"),120,50,0x999999,false);
        var mould=menu.entity.filter.getStackInSlot(0);var name=mould.isEmpty()?Component.translatable("ttmisc.all"):SoulMouldItem.patternName(mould);
        graphics.drawString(font,name,62-font.width(name)/2,16,0x999999,false);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
