package automagy.client;

import automagy.content.PortableCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.aspect.TCAspects;

public final class PortableCraftingScreen extends AbstractContainerScreen<PortableCraftingMenu>{
 private static final ResourceLocation TEXTURE=ResourceLocation.parse("automagy:textures/gui/portable_crafting.png");
 private static final int[][] POSITIONS={{64,2},{16,24},{16,83},{64,105},{112,83},{112,24}};
 private float craftAlpha=.138f;
 public PortableCraftingScreen(PortableCraftingMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=190;imageHeight=206;}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
  // The original circle fades in, then changes from white to pale green per rendered frame.
  craftAlpha=Math.min(2,craftAlpha+(craftAlpha<1?.01f:.005f));
  tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+135,topPos-6,135,0,55,135);
  tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos+123,0,135,190,89);
  float tintProgress=Math.max(0,craftAlpha-1);
  g.setColor(1-.5f*tintProgress,1-.2f*tintProgress,1-.5f*tintProgress,Math.min(1,craftAlpha));
  tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos-6,0,0,135,135);
  g.setColor(1,1,1,1);
  tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+15+menu.ownerSlot()*18,topPos+187,0,224,menu.ownerSlot()==8?18:17,18);
  boolean insufficient=false;
  for(int i=0;i<6;i++){
   int cost=menu.displayedCost(i);if(cost<=0)continue;
   var aspect=TCAspects.catalog().require(PortableCraftingMenu.PRIMALS[i]);int tint=aspect.color();
   boolean paid=menu.affordable(i);insufficient|=!paid;
   float alpha=paid?1:.3f+.2f*net.minecraft.util.Mth.sin((minecraft.player.tickCount+i*10)/2f);
   g.setColor((tint>>16&255)/255f,(tint>>8&255)/255f,(tint&255)/255f,alpha);
   int ax=leftPos+POSITIONS[i][0],ay=topPos+POSITIONS[i][1];
   tc4revival.client.LegacyGui.blit(g,aspect.texture(),ax,ay,0,0,16,16,16,16);g.setColor(1,1,1,1);
   String amount=java.math.BigDecimal.valueOf(cost,2).stripTrailingZeros().toPlainString();
   g.pose().pushPose();g.pose().translate(ax,ay,0);g.pose().scale(.5f,.5f,1);
   int tx=32-font.width(amount),ty=32-font.lineHeight;
   g.drawString(font,amount,tx-1,ty,0xff000000,false);g.drawString(font,amount,tx+1,ty,0xff000000,false);
   g.drawString(font,amount,tx,ty-1,0xff000000,false);g.drawString(font,amount,tx,ty+1,0xff000000,false);
   g.drawString(font,amount,tx,ty,0xffffffff,false);g.pose().popPose();
  }
  var preview=menu.unavailablePreview();
  if(!preview.isEmpty()){
   g.setColor(.33f,.33f,.33f,.66f);g.renderItem(preview,leftPos+160,topPos+53);
   g.renderItemDecorations(font,preview,leftPos+160,topPos+53);g.setColor(1,1,1,1);
  }
  if(menu.usingStaff()){message(g,"staff.1",32);message(g,"staff.2",38);}
  else if(insufficient)message(g,"visLow",35);
 }
 private void message(GuiGraphics g,String key,int y){
  var text=Component.translatable("gui.automagy.crafting."+key);g.pose().pushPose();g.pose().translate(leftPos+168,topPos+y,0);g.pose().scale(.5f,.5f,1);g.drawString(font,text,-font.width(text)/2,0,0xee6e6e,false);g.pose().popPose();
 }
 @Override public void render(GuiGraphics g,int x,int y,float partial){
  renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);
  for(int i=0;i<6;i++)if(menu.displayedCost(i)>0&&isHovering(POSITIONS[i][0],POSITIONS[i][1],16,16,x,y))
   g.renderTooltip(font,Component.literal(thaumcraft.api.aspects.AspectViews.display(TCAspects.catalog().require(PortableCraftingMenu.PRIMALS[i])).displayName()),x,y);
 }
}
