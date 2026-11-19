package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.TravelingTrunkMenu;

public  class TravelingTrunkScreen extends AbstractContainerScreen<TravelingTrunkMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/guitrunkbase.png");
    public TravelingTrunkScreen(TravelingTrunkMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); imageHeight=200; }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        var trunk=menu.trunk();
        LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        LegacyGui.blit(graphics,TEXTURE,leftPos+134,topPos+2,176,16,Math.round(trunk.getHealth()/trunk.getMaxHealth()*39),6);
        if (menu.rows()==4) LegacyGui.blit(graphics,TEXTURE,leftPos,topPos+80,0,206,imageWidth,27);
        if (trunk.stay()) LegacyGui.blit(graphics,TEXTURE,leftPos+112,topPos,176,0,10,10);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        graphics.pose().pushPose(); graphics.pose().scale(.5f,.5f,.5f);
        graphics.drawString(font,Component.literal(menu.trunk().ownerName()).append(Component.translatable("entity.trunk.guiname")),8,4,12624112,false);
        graphics.pose().popPose();
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics); super.render(graphics,mouseX,mouseY,partial); renderTooltip(graphics,mouseX,mouseY);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        boolean handled=super.mouseClicked(x,y,button);
        if (x>=leftPos+112 && x<leftPos+122 && y>=topPos && y<=topPos+10 && minecraft!=null && minecraft.player!=null && minecraft.gameMode!=null) {
            var trunk=menu.trunk();
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(),.6f+(trunk.stay()?0:.2f),.3f));
            minecraft.player.displayClientMessage(Component.translatable(trunk.stay()?"entity.trunk.move":"entity.trunk.stay"),false);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1); return true;
        }
        return handled;
    }
    @Override protected void containerTick() { super.containerTick(); if (!menu.trunk().isAlive()) onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
