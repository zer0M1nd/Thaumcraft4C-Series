package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.DeconstructionTableMenu;

public  class DeconstructionTableScreen extends AbstractContainerScreen<DeconstructionTableMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_decontable.png");
    public DeconstructionTableScreen(DeconstructionTableMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int height=menu.remaining()*46/40;
        if (height>0) LegacyGui.blit(g,TEXTURE,leftPos+93,topPos+15+46-height,176,46-height,9,height);
        var aspect=menu.pending();
        if (aspect!=null) {
            int color=thaumcraft.api.client.ClientAspectViews.view(aspect).color(); g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            LegacyGui.blit(g,thaumcraft.api.client.ClientAspectViews.view(aspect).texture(),leftPos+64,topPos+48,0,0,16,16,16,16); g.setColor(1,1,1,1);
        }
    }
    private boolean overAspect(double x,double y) { return x>=leftPos+64 && x<leftPos+80 && y>=topPos+48 && y<topPos+64; }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        renderBackground(g); super.render(g,x,y,partial); renderTooltip(g,x,y);
        var aspect=menu.pending();
        if (aspect!=null && overAspect(x,y)) g.renderComponentTooltip(font,java.util.List.of(Component.literal(thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()),Component.translatable(thaumcraft.api.client.ClientAspectViews.view(aspect).descriptionKey())),x,y-8);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if (menu.pending()!=null && overAspect(x,y) && minecraft.gameMode!=null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(tc4revival.content.TCSounds.HH_OFF.get(),1+minecraft.player.getRandom().nextFloat()*.1f,.2f));
            return true;
        }
        return super.mouseClicked(x,y,button);
    }
}
