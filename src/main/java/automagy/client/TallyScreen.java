package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TallyScreen extends AbstractContainerScreen<TallyMenu>{
 private static final ResourceLocation TEXTURE=LegacyControls.texture("filter_holder");
 public TallyScreen(TallyMenu m,Inventory i,Component t){super(m,i,t);}
 @Override protected void init(){super.init();addRenderableWidget(LegacyControls.checkbox(leftPos+104,topPos+32,TEXTURE,()->menu.value(0)==0,r->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)));}
 @Override protected void renderBg(GuiGraphics g,float p,int x,int y){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,176,166);LegacyControls.text(g,Component.translatable("Automagy.gui.tallyBlock.allowPartialMatch"),leftPos+117,topPos+34,.75f,0xffffff);
  var stack=menu.getSlot(0).getItem();if(stack.getItem() instanceof FilterItem f){String[] keys={"desc."+(f.blacklist?"black":"white"),"desc."+(menu.value(0)==0?"any":"all"),"quantityResult."+(f.blacklist?"black":"white")};int[] yy={51,57,66};for(int i=0;i<(ItemFilter.load(stack).useCount?3:2);i++){var text=Component.translatable("Automagy.gui.tallyBlock."+keys[i]);LegacyControls.text(g,text,leftPos+88-(int)(font.width(text)*.2625),topPos+yy[i],.525f,0xffffff);}}
 }
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
