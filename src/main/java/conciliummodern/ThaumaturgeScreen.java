package conciliummodern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.client.LegacyGui;

public  class ThaumaturgeScreen extends AbstractContainerScreen<ThaumaturgeMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_pech.png");
    public ThaumaturgeScreen(ThaumaturgeMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) { LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight); if (menu.ready()) LegacyGui.blit(g,TEXTURE,leftPos+67,topPos+24,176,0,25,25); }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {}
    @Override public void render(GuiGraphics g,int x,int y,float partial) { renderBackground(g); super.render(g,x,y,partial); renderTooltip(g,x,y); }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if (x>=leftPos+67 && x<leftPos+92 && y>=topPos+24 && y<topPos+49 && menu.ready()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
            minecraft.player.playSound(tc4revival.content.TCSounds.PECH_DICE.get(),.5f,.95f+minecraft.level.random.nextFloat()*.1f); return true;
        }
        return super.mouseClicked(x,y,button);
    }
}

