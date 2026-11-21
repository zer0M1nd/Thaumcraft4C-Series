package advancedthaumaturgy.client;
import advancedthaumaturgy.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class WandbenchScreen extends AbstractContainerScreen<WandbenchMenu>{
    public WandbenchScreen(WandbenchMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){g.blit(AdvancedThaumaturgy.id("textures/gui/wandbench.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,8,4,0x404040,false);var plan=menu.entity.plan();if(plan!=null)g.drawString(font,Component.translatable("advancedthaumaturgy.bench.cost",plan.cost()),8,68,0x404040,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
