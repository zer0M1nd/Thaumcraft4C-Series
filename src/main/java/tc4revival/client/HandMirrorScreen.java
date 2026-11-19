package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.HandMirrorMenu;

public  class HandMirrorScreen extends AbstractContainerScreen<HandMirrorMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/guihandmirror.png");
    public HandMirrorScreen(HandMirrorMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) { LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight); }
    @Override protected boolean checkHotbarKeyPressed(int key,int scan) { return false; }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) { renderBackground(graphics); super.render(graphics,x,y,partial); renderTooltip(graphics,x,y); }
}
