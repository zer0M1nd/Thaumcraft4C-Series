package thmodern.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import thmodern.soul.SoulDeviceMenu;
public final class SoulDeviceScreen extends AbstractContainerScreen<SoulDeviceMenu> {
    public SoulDeviceScreen(SoulDeviceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=menu.kind==1?219:166;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        {
            var texture=net.minecraft.resources.ResourceLocation.parse("thaumichorizons:textures/gui/"+(menu.kind==0?"guisieve":"guiinspiratron")+".png");
            tc4revival.client.LegacyGui.blit(g,texture,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
            int height=menu.kind==0?menu.progress()*39/1200:menu.progress()*28/100;
            if(height>0)tc4revival.client.LegacyGui.blit(g,texture,leftPos+(menu.kind==0?91:66),topPos+(menu.kind==0?58-height:102),176,(menu.kind==0?166:158)-height,menu.kind==0?35:44,height,256,256);
            return;
        }

    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
