package automagy.client;
import automagy.content.MirrorSettingsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class MirrorSettingsScreen extends AbstractContainerScreen<MirrorSettingsMenu>{
 public MirrorSettingsScreen(MirrorSettingsMenu m,Inventory i,Component t){super(m,i,t);}
 protected void renderBg(GuiGraphics g,float p,int x,int y){if(menu.showOutput())tc4revival.client.LegacyGui.blit(g,LegacyControls.texture("req_eyes"),leftPos,topPos,0,0,176,166);
 else{tc4revival.client.LegacyGui.blit(g,LegacyControls.texture("req_eyes"),leftPos,topPos,0,0,96,88);tc4revival.client.LegacyGui.blit(g,LegacyControls.texture("req_eyes"),leftPos,topPos+78,0,78,176,166);}}
 protected void renderLabels(GuiGraphics g,int x,int y){for(int i=0;i<(menu.showOutput()?2:1);i++){
  LegacyControls.text(g,Component.translatable("Automagy.gui.scribeMirror."+(i==0?"input":"output")),42+i*88,31,.75f,0xffffff,true);
  int state=menu.status(i);if(state!=0)LegacyControls.text(g,Component.translatable("Automagy.gui.scribeMirror."+switch(state){case 1->"valid";case 2->"outranged";case 3->"noMirror";default->"unlinked";}),42+i*88,40,.6f,state==1?0x00cc00:0xff0000,true);
 }}
 public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
