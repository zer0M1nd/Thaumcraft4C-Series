package gadomancymodern.client;

import gadomancymodern.automation.PackagerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original 190x234 layout, including 23px crafting row spacing and 1/2-size option labels. */
public class PackagerScreen extends AbstractContainerScreen<PackagerMenu> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("gadomancy","textures/gui/gui_packager.png");
    public PackagerScreen(PackagerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=190;imageHeight=234;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        tc4revival.client.LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int progress=menu.machine.progress();if(progress>0)tc4revival.client.LegacyGui.blit(graphics,TEXTURE,leftPos+89,topPos+67,210,0,Math.min(47,progress),9);
        boolean[] active={menu.machine.useEssentia,menu.machine.autoStart,menu.machine.disguise};
        String[] keys={"useEssentia","autoStart","disguisePackage"};
        for(int i=0;i<3;i++){
            int y=97+13*i;if(active[i])tc4revival.client.LegacyGui.blit(graphics,TEXTURE,leftPos+91,topPos+y,249,10,8,8);
            graphics.pose().pushPose();graphics.pose().translate(leftPos+100,topPos+y+1,0);graphics.pose().scale(.5f,.5f,1);
            graphics.drawString(font,Component.translatable("gadomancy.info.ArcanePackager."+keys[i]),0,0,active[i]?0xffffff:0x808080,false);graphics.pose().popPose();
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&x>=leftPos+90&&x<leftPos+98)for(int option=0;option<3;option++)if(y>=topPos+97+13*option&&y<topPos+105+13*option){
            boolean active=switch(option){case 0->menu.machine.useEssentia;case 1->menu.machine.autoStart;default->menu.machine.disguise;};
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,option*2+(active?1:0));return true;
        }return super.mouseClicked(x,y,button);
    }
}
