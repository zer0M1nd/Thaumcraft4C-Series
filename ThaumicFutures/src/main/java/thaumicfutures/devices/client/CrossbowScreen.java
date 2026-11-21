package thaumicfutures.devices.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumicfutures.devices.CrossbowMenu;

public class CrossbowScreen extends AbstractContainerScreen<CrossbowMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicfutures:textures/gui/crossbow.png");
    public CrossbowScreen(CrossbowMenu menu,Inventory inventory,Component title) { super(menu,inventory,title);imageWidth=176;imageHeight=166; }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        g.blit(TEXTURE,leftPos+68,topPos+59,192,48,Math.round(39*menu.turret.getHealth()/menu.turret.getMaxHealth()),6);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) { g.drawString(font,title,8,6,0xeed8b4,false);g.drawString(font,playerInventoryTitle,8,72,0xeed8b4,false); }
    @Override public void render(GuiGraphics g,int x,int y,float partial) { renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y); }
}
