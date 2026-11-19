package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.AlchemyFurnaceMenu;

public  class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_alchemyfurnace.png");
    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend(); LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int flame=Math.min(20,menu.value(1)*20/Math.max(1,menu.value(2)==0 ? 200 : menu.value(2)));
        if (menu.value(1)>0) LegacyGui.blit(graphics,TEXTURE,leftPos+80,topPos+46-flame,176,20-flame,16,flame);
        int cook=Math.min(46,menu.value(0)*46/Math.max(1,menu.value(4)));
        LegacyGui.blit(graphics,TEXTURE,leftPos+106,topPos+59-cook,216,46-cook,9,cook);
        int contents=Math.min(48,menu.value(3)*48/50); LegacyGui.blit(graphics,TEXTURE,leftPos+61,topPos+60-contents,200,48-contents,8,contents);
        LegacyGui.blit(graphics,TEXTURE,leftPos+60,topPos+8,232,0,10,55); com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) { renderBackground(graphics); super.render(graphics,x,y,partial); renderTooltip(graphics,x,y); }
}
