package temodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import temodern.crafting.GeneratorMenu;

public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    public GeneratorScreen(GeneratorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=210;imageHeight=120;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        tc4revival.client.LegacyGui.blit(g,new net.minecraft.resources.ResourceLocation("thaumicenergistics","textures/modern/gui/essentia_resonator.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        int fuel=menu.data.get(0),kind=menu.data.get(1),remaining=menu.data.get(2),total=Math.max(1,menu.data.get(3));
        bar(g,28,fuel,64,kind==2?0xffcdddff:0xffff7136);
        bar(g,62,remaining,total,0xff57bada);
        g.drawString(font,Component.translatable("gui.thaumicenergistics.fuel",fuel),leftPos+9,topPos+17,0x303030,false);
        g.drawString(font,Component.translatable("gui.thaumicenergistics.remaining",remaining),leftPos+9,topPos+49,0x303030,false);
        g.drawString(font,Component.translatable("gui.thaumicenergistics.generation",menu.data.get(4)/100d,menu.data.get(5)),leftPos+9,topPos+83,0x303030,false);
    }
    private void bar(GuiGraphics g,int y,int amount,int capacity,int color){g.fill(leftPos+10,topPos+y+1,leftPos+10+(int)(190L*Math.max(0,Math.min(amount,capacity))/capacity),topPos+y+9,color);}
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){g.drawString(font,title,8,5,0x303030,false);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
}
