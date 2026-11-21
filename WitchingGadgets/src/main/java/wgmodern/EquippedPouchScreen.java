package wgmodern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
public class EquippedPouchScreen extends AbstractContainerScreen<EquippedPouchMenu> {
 public EquippedPouchScreen(EquippedPouchMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){tc4revival.client.LegacyGui.blit(g,new ResourceLocation("thaumcraft","textures/gui/gui_focuspouch.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
 @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
