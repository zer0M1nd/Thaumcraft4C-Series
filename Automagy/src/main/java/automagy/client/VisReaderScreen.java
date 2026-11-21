package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class VisReaderScreen extends AbstractContainerScreen<VisReaderMenu>{
 private static final ResourceLocation TEXTURE=LegacyControls.texture("vis_reader");
 private final Button[][] buttons=new Button[4][3];
 public VisReaderScreen(VisReaderMenu m,Inventory i,Component t){super(m,i,t);imageHeight=178;}
 private Component label(int r,int f){int v=menu.value(r,f);return f==0?Component.translatable("gui.automagy.reader.mode."+v):f==1?(v==0?Component.translatable("gui.automagy.reader.auto"):Component.literal(""+v)):Component.translatable("gui.automagy.reader.direction."+v);}
 @Override protected void init(){super.init();int[] x={29,86,123},w={56,36,27};
  for(int row=0;row<4;row++){int r=row;for(int f=0;f<3;f++){int field=f;buttons[r][f]=addRenderableWidget(LegacyControls.small(leftPos+x[f],topPos+11+19*r,w[f],15,()->label(r,field),right->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,r*4+field+(right?16:0))));}
   addRenderableWidget(LegacyControls.checkbox(leftPos+155,topPos+14+19*r,TEXTURE,()->menu.value(r,3)==1,right->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,r*4+3)));
  }
 }
 @Override protected void containerTick(){super.containerTick();for(int r=0;r<4;r++){buttons[r][1].active=menu.value(r,0)!=0&&menu.value(r,0)!=4;buttons[r][2].active=menu.value(r,0)!=0;}}
 @Override protected void renderBg(GuiGraphics g,float p,int x,int y){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,176,178);String[] key={"mode.col","capacity.col","dir.col","invert"};int[] xx={31,88,125,153};for(int i=0;i<4;i++)LegacyControls.text(g,Component.translatable("Automagy.gui.visReader."+key[i]),leftPos+xx[i],topPos,.75f,0xffffff);}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
