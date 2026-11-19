package thaumicomniverse.compat.thermal;
import cofh.thermal.lib.client.gui.DynamoScreen;
import cofh.thermal.core.client.gui.ThermalGuiHelper;
import cofh.core.util.helpers.GuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ThermalClient {
    public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(ThermalContent.MENU.get(),Screen::new));}
    public static class Screen extends DynamoScreen<EssentiaDynamoMenu> {
        public Screen(EssentiaDynamoMenu menu,Inventory inventory,Component title){
            super(menu,inventory,menu.tile,title);
            texture=ResourceLocation.parse("thermal:textures/gui/container/item_dynamo.png");
            info=GuiHelper.generatePanelInfo("info.thaumicomniverse.essentia_dynamo");
        }
        @Override public void init(){
            super.init();addElement(ThermalGuiHelper.createDefaultDuration(this,80,35,GuiHelper.SCALE_FLAME,tile));
        }
        @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){
            super.renderLabels(graphics,mouseX,mouseY);
            graphics.drawString(font,((EssentiaDynamo)tile).contents().total()+"/64",36,58,0x604078,false);
        }
    }
    private ThermalClient(){}
}
