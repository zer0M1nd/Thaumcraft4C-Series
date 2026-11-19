package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HourglassScreen extends AbstractContainerScreen<HourglassMenu>{
 private static final ResourceLocation TEXTURE=LegacyControls.texture("checkboxable");
 public HourglassScreen(HourglassMenu m,Inventory i,Component t){super(m,i,t);}
 private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 @Override protected void init(){super.init();
  addRenderableWidget(LegacyControls.small(leftPos+38,topPos+12,40,12,.5f,()->Component.translatable("Automagy.gui.hourglassMagic.timer.min"),r->send(10+(hasShiftDown()?2:0)+(r?1:0))));
  addRenderableWidget(LegacyControls.small(leftPos+80,topPos+12,40,12,.5f,()->Component.translatable("Automagy.gui.hourglassMagic.timer.sec"),r->send(14+(hasShiftDown()?2:0)+(r?1:0))));
  String[] key={"signal","sandFlip","signalReact"};
  for(int n=0;n<3;n++){int row=n;addRenderableWidget(LegacyControls.small(leftPos+75,topPos+31+19*n,95,15,()->Component.translatable("Automagy.gui.hourglassMagic."+key[row]+"."+menu.value(row+1)),r->send(20+row*2+(r?1:0))));}
  addRenderableWidget(LegacyControls.checkbox(leftPos+145,topPos+10,TEXTURE,()->menu.value(4)==1,r->send(7)));
 }
 @Override protected void renderBg(GuiGraphics g,float p,int mx,int my){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos+20,0,0,imageWidth,imageHeight);
  String[] key={"timer","signal","sandFlip","signalReact"};int[] y={1,35,55,75};
  for(int i=0;i<4;i++)LegacyControls.text(g,Component.translatable("Automagy.gui.hourglassMagic."+key[i]+".name"),leftPos+2,topPos+y[i],.75f,0xffffff);
  LegacyControls.centered(g,Component.translatable("Automagy.gui.hourglassMagic.repeat"),leftPos+149,topPos+1,.75f,0xffffff);
  int t=menu.value(0);
  Component time=t>=60?Component.translatable("Automagy.gui.hourglassMagic.timer.minutesAndSeconds",t/60,t%60)
      :t==1?Component.translatable("Automagy.gui.hourglassMagic.timer.secondsOne")
      :Component.translatable("Automagy.gui.hourglassMagic.timer.seconds",t);
  g.drawString(font,time,leftPos+40,topPos,0xffffff,true);
 }
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public void render(GuiGraphics g,int x,int y,float p){
  renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);
  if(isHovering(38,12,40,12,x,y)||isHovering(80,12,40,12,x,y)){
   var lines=java.util.Arrays.stream(Component.translatable("Automagy.gui.hourglassMagic.timer.tooltip").getString()
       .replace("\\n","\n").split("\n")).map(Component::literal).map(Component::getVisualOrderText).toList();
   g.pose().pushPose();
   g.pose().translate(leftPos,topPos,0);
   g.pose().scale(.75f,.75f,1);
   g.renderTooltip(font,lines,(int)((x-leftPos-10)/.75f),(int)((y-topPos+20)/.75f));
   g.pose().popPose();
  }
 }
}
