package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GolemWorkbenchScreen extends AbstractContainerScreen<GolemWorkbenchMenu>{
 private static final ResourceLocation TEXTURE=LegacyControls.texture("golem_workbench");
 public GolemWorkbenchScreen(GolemWorkbenchMenu m,Inventory i,Component t){super(m,i,t);imageHeight=205;}
 @Override protected void renderBg(GuiGraphics g,float p,int mx,int my){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,176,205);
  if(menu.value(3)!=0)tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+7,topPos+85,184,85,17,17);
  // The legacy ghost grid has no separate output preview.
  if(menu.value(1)>1){for(int n=0;n<2;n++)tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+64+n*24,topPos+68,n*24,isHovering(64+n*24,68,24,8,mx,my)?218:236,24,8);LegacyControls.centered(g,Component.literal((menu.value(0)+1)+"/"+menu.value(1)),leftPos+120,topPos+70,.5f,0xffffff);}
 }
 @Override public boolean mouseClicked(double x,double y,int b){if((b==0||b==1)&&menu.value(1)>1&&isHovering(64,68,48,8,x,y)){int next=Math.floorMod(menu.value(0)+(x<leftPos+88?-1:1),menu.value(1));minecraft.gameMode.handleInventoryButtonClick(menu.containerId,next);return true;}return super.mouseClicked(x,y,b);}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
