package thmodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thmodern.blood.InjectorMenu;

public final class InjectorScreen extends AbstractContainerScreen<InjectorMenu> {
    public InjectorScreen(InjectorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=191;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse("thaumichorizons:textures/gui/guiinjector.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
