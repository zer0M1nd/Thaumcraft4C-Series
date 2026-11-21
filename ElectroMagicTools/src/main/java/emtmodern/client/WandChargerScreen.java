package emtmodern.client;
import emtmodern.energy.WandChargerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class WandChargerScreen extends AbstractContainerScreen<WandChargerMenu> {
    public WandChargerScreen(WandChargerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=165;}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(emtmodern.ElectroMagicTools.WAND_CHARGER_MENU.get(),WandChargerScreen::new));}
    @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY){tc4revival.client.LegacyGui.blit(gui,ResourceLocation.parse("emt:textures/guis/wandcharger.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics gui,int x,int y){gui.drawString(font,Component.translatable("gui.EMT.wandRecharge.title"),6,6,0x404040,false);gui.drawString(font,Component.translatable("gui.EMT.inventory"),8,imageHeight-94,0x404040,false);}
    @Override public void render(GuiGraphics gui,int x,int y,float partial){renderBackground(gui);super.render(gui,x,y,partial);renderTooltip(gui,x,y);}
}
