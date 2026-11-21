package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class MawScreen extends AbstractContainerScreen<MawMenu>{
 public MawScreen(MawMenu m,Inventory i,Component t){super(m,i,t);}
 protected void renderBg(GuiGraphics g,float p,int x,int y){tc4revival.client.LegacyGui.blit(g,LegacyControls.texture("filter_holder"),leftPos,topPos,0,0,176,166);}
 protected void renderLabels(GuiGraphics g,int x,int y){var stack=menu.getSlot(0).getItem();if(stack.getItem() instanceof FilterItem item&&ItemFilter.load(stack).useCount){
  LegacyControls.centered(g,Component.translatable("Automagy.gui.finicalMaw.quantityResult.open"),88,51,.75f*.7f,0xffffff,true);
  LegacyControls.centered(g,Component.translatable("Automagy.gui.finicalMaw.quantityResult."+(item.blacklist?"leave":"limit")),88,57,.75f*.7f,0xffffff,true);
 }}
 public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
