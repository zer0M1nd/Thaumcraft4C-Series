package thmodern.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import thmodern.lens.LensCaseMenu;
public final class LensCaseScreen extends AbstractContainerScreen<LensCaseMenu> {
    private static final net.minecraft.resources.ResourceLocation TEXTURE=net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/gui/gui_focuspouch.png");
    public LensCaseScreen(LensCaseMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=233;}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,176,233,256,256);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){if(menu.locked<9)tc4revival.client.LegacyGui.blit(g,TEXTURE,8+menu.locked*18,209,240,0,16,16,256,256);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
