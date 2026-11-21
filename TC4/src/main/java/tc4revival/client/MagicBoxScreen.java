package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.MagicBoxMenu;

public  class MagicBoxScreen extends AbstractContainerScreen<MagicBoxMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");
    public MagicBoxScreen(MagicBoxMenu menu,Inventory inventory,Component title) { super(menu,inventory,title);imageHeight=168;inventoryLabelY=74; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("<"),button->changePage(0))
                .bounds(leftPos+138,topPos+2,16,14).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(">"),button->changePage(1))
                .bounds(leftPos+155,topPos+2,16,14).build());
    }
    private void changePage(int button) {
        if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {
        super.renderLabels(graphics,x,y);
        graphics.drawString(font,(menu.page()+1)+"/"+menu.pageCount(),104,6,0x404040,false);
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,176,71);LegacyGui.blit(graphics,TEXTURE,leftPos,topPos+71,0,126,176,96);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) { renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y); }
}
