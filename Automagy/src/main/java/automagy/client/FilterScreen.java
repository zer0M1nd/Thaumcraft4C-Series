package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FilterScreen extends AbstractContainerScreen<FilterMenu>{
 private EditBox name;private ResourceLocation texture;
 public FilterScreen(FilterMenu m,Inventory i,Component t){super(m,i,t);imageHeight=180;}
 @Override protected void init(){super.init();texture=LegacyControls.texture(menu.blacklist()?"runic_blacklist":"runic_whitelist");
  for(int i=0;i<3;i++){int n=i;addRenderableWidget(LegacyControls.checkbox(leftPos+80,topPos+18+i*10,texture,()->n==0?menu.value(9)==0:menu.value(n+9)==1,r->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,n)));}
  name=new LegacyEditBox(font,leftPos+81,topPos+59,100,16,Component.translatable("Automagy.gui.filter.byName"));name.setMaxLength(ItemFilter.MAX_NAME);name.setValue(menu.name());name.setResponder(s->{menu.initialName(s);FilterNetwork.rename(menu.containerId,s);});addRenderableWidget(name);
 }
 @Override protected void containerTick(){super.containerTick();name.tick();}
 @Override public boolean keyPressed(int k,int scan,int mods){if(k!=256&&name.isFocused()){name.keyPressed(k,scan,mods);return true;}return super.keyPressed(k,scan,mods);}
 @Override public boolean mouseClicked(double x,double y,int b){if(b==1&&name.isMouseOver(x,y)){name.setValue("");return true;}return super.mouseClicked(x,y,b);}
 @Override protected void renderBg(GuiGraphics g,float p,int x,int y){tc4revival.client.LegacyGui.blit(g,texture,leftPos,topPos,0,0,176,180);
  if(menu.ownerSlot()<9)tc4revival.client.LegacyGui.blit(g,texture,leftPos+7+menu.ownerSlot()*18,topPos+155,0,180,menu.ownerSlot()==8?18:17,18);
  String[] key={"ignoreQuantity","ignoreMetadata","ignoreNBT"};int color=menu.blacklist()?0xffffff:0;
  for(int i=0;i<3;i++)LegacyControls.text(g,Component.translatable("Automagy.gui.filter."+key[i]),leftPos+91,topPos+20+i*10,.75f,color);
  LegacyControls.text(g,Component.translatable("Automagy.gui.filter.byName"),leftPos+81,topPos+50,.75f,color);
 }
 @Override protected void renderLabels(GuiGraphics g,int x,int y){if(menu.value(9)==1)for(int i=0;i<9;i++)if(menu.getSlot(i).hasItem()){var s=menu.getSlot(i);g.pose().pushPose();g.pose().translate(s.x+17,s.y+11,200);g.pose().scale(.5f,.5f,1);String n=""+menu.value(i);g.drawString(font,n,-font.width(n),0,0xffffff,true);g.pose().popPose();}}
 @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);
  if(name.isMouseOver(x,y)){
   var lines=java.util.Arrays.stream(Component.translatable("Automagy.gui.filter.textFilterTooltip").getString().split("\\\\n|\\n")).map(s->Component.literal(s).getVisualOrderText()).toList();
   g.pose().pushPose();g.pose().translate(leftPos,topPos,0);g.pose().scale(.75f,.75f,1);
   g.renderTooltip(font,lines,(int)((x-leftPos-20)/.75f),(int)((y-topPos+20)/.75f));g.pose().popPose();
  }
 }
}
