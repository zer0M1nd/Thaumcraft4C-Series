package ttmodern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class IchorPouchScreen extends AbstractContainerScreen<IchorPouchMenu> {
    public IchorPouchScreen(IchorPouchMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=256;imageHeight=256;}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Tinkerer.ICHOR_POUCH_MENU.get(),IchorPouchScreen::new));}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse(Tinkerer.ID+":textures/gui/ichor_pouch.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
