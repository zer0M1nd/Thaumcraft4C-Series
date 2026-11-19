package thmodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thmodern.vat.VatMenu;

public final class VatScreen extends AbstractContainerScreen<VatMenu> {
    public VatScreen(VatMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=209;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){
        tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse("thaumichorizons:textures/gui/guivat.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        if(!menu.allowed())tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse("thaumichorizons:textures/gui/guivat.png"),leftPos+58,topPos+30,176,163,57,20);
        var texture=ResourceLocation.parse("thaumichorizons:textures/gui/guivat.png");
        // Nine hearts per row, then a four-column rail of the legacy infusion icons.
        for(int i=0;i<Math.min(27,(int)Math.ceil(menu.maximumHealth()/2));i++){
            int px=leftPos+56+7*(i%9),py=topPos+12+7*(i/9);float filled=menu.health()/2-i;
            tc4revival.client.LegacyGui.blit(graphics,texture,px,py,176,120,7,6);
            if(filled>0)tc4revival.client.LegacyGui.blit(graphics,texture,px,py,176,126,filled>=1?7:4,6);
        }
        int shown=0;for(int i=0;i<10;i++)if((menu.creatureInfusions()&(1<<i))!=0){tc4revival.client.LegacyGui.blit(graphics,texture,leftPos+55+16*(shown%4),topPos+56+17*(shown/4),i*16,209,16,16);shown++;}
        for(int i=0;i<10;i++)if((menu.selfInfusions()&(1<<i))!=0){tc4revival.client.LegacyGui.blit(graphics,texture,leftPos+55+16*(shown%4),topPos+56+17*(shown/4),i*16,225,16,16);shown++;}
        if(menu.mode()==1)graphics.drawCenteredString(font,Component.translatable("thaumichorizons.vat.cloning",menu.progress()),leftPos+88,topPos+105,0xffffff);
        if(menu.mode()==3)graphics.drawCenteredString(font,Component.translatable("thaumichorizons.vat.effigy_growing",menu.progress()),leftPos+88,topPos+105,0xffffff);
        if(menu.mode()==4)graphics.drawCenteredString(font,Component.translatable("thaumichorizons.vat.effigy_ready"),leftPos+88,topPos+105,0xffffff);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
