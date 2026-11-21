package conciliummodern;

public final class AstralMonitorScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<AstralMonitorMenu>{
    private static final net.minecraft.resources.ResourceLocation TEXTURE=net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:textures/gui/astral_monitor.png");
    public AstralMonitorScreen(AstralMonitorMenu menu,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.network.chat.Component title){super(menu,inventory,title);}
    @Override protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics,float partial,int mouseX,int mouseY){tc4revival.client.LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY){}
    @Override public void render(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partial){renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);}
}
