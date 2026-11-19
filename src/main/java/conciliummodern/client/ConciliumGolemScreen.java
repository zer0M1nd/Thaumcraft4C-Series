package conciliummodern.client;

import conciliummodern.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.client.*;
import tc4revival.content.*;

public final class ConciliumGolemScreen extends AbstractContainerScreen<GolemMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/guigolem.png");
    public ConciliumGolemScreen(GolemMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    private boolean valet(){return ConciliumGolems.VALET.equals(menu.golem().configuration().customTask());}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);var config=menu.golem().configuration();
        for(int i=0;i<Math.min(6,menu.sampleCount());i++){
            int x=leftPos+96+i/2*28,y=topPos+12+i%2*31;LegacyGui.blit(g,TEXTURE,x,y,184,config.material().ordinal()*24,24,24);
            if(valet()&&config.upgrades(4)>0){LegacyGui.blit(g,TEXTURE,x,y-8,72,168,24,12);int color=menu.golem().filters().color(i+menu.page()*6);
                if(color>=0){int rgb=TallowCandleBlock.COLORS[color];g.setColor((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,1);LegacyGui.blit(g,TEXTURE,x+9,y-5,0,176,6,6);g.setColor(1,1,1,1);}}
        }
        if(menu.maxPage()>0){LegacyGui.blit(g,TEXTURE,leftPos+111,topPos+68,0,menu.page()>0?200:208,24,8);LegacyGui.blit(g,TEXTURE,leftPos+135,topPos+68,24,menu.page()<menu.maxPage()?200:208,24,8);}
        if(valet())for(int i=0;i<3;i++){
            LegacyGui.blit(g,TEXTURE,leftPos+42,topPos+40+i*10,8,168,8,8);if(!config.toggle(i))LegacyGui.blit(g,TEXTURE,leftPos+42,topPos+40+i*10,8,176,8,8);
            small(g,Component.translatable("gui.thaumicconcilium.valet."+i+"."+config.toggle(i)),leftPos+52,topPos+42+i*10);
        }
        ConciliumGolemPreview.render(g,menu.golem());
    }
    private void small(GuiGraphics g,Component label,int x,int y){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.5f,.5f,.5f);g.drawString(font,label,0,0,0xDDDDDD,false);g.pose().popPose();}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.pose().pushPose();g.pose().scale(.5f,.5f,.5f);int lineY=22;
        for(var line:font.split(Component.translatable(valet()?"tc.valet.gui.text":"tc.assistant.gui.text"),110)){g.drawString(font,line,80,lineY,0xDDDDDD,false);lineY+=font.lineHeight;}
        if(menu.maxPage()>0)g.drawString(font,(menu.page()+1)+"/"+(menu.maxPage()+1),323,140,0xDDDDDD,false);g.pose().popPose();
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
    private boolean inside(double x,double y,int bx,int by,int w,int h){return x>=leftPos+bx&&x<leftPos+bx+w&&y>=topPos+by&&y<topPos+by+h;}
    private boolean send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);return true;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(valet()){
            if(menu.golem().configuration().upgrades(4)>0)for(int i=0;i<Math.min(6,menu.sampleCount());i++){
                if(inside(x,y,96+i/2*28,4+i%2*31,8,12))return send(i+menu.page()*6);
                if(inside(x,y,112+i/2*28,4+i%2*31,8,12))return send(i+menu.page()*6+menu.sampleCount());
            }
            for(int i=0;i<3;i++)if(inside(x,y,42,40+i*10,8,8))return send(50+i);
        }
        if(menu.page()>0&&inside(x,y,111,68,24,8))return send(66);if(menu.page()<menu.maxPage()&&inside(x,y,135,68,24,8))return send(67);
        return super.mouseClicked(x,y,button);
    }
}
