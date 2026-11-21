package wgmodern;
import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;import net.minecraft.world.entity.player.Inventory;import net.minecraft.network.chat.Component;import net.minecraft.resources.ResourceLocation;
public class CuttingScreen extends AbstractContainerScreen<CuttingMenu>{
 private static final ResourceLocation BG=new ResourceLocation("witchinggadgets","textures/gui/cuttingtable.png"),CUTS=new ResourceLocation("witchinggadgets","textures/gui/research/gemcuts_transparent.png");
 public CuttingScreen(CuttingMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=166;}
 @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){tc4revival.client.LegacyGui.blit(g,BG,leftPos,topPos,0,0,176,166);int offset=menu.tile.cut==0?0:80;com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();tc4revival.client.LegacyGui.blit(g,CUTS,leftPos+73,topPos+17,30,30,offset,offset,80,80,255,255);var a=thaumcraft.api.aspects.Aspect.getAspect(menu.tile.aspect());if(a!=null){g.setColor((a.getColor()>>16&255)/255f,(a.getColor()>>8&255)/255f,(a.getColor()&255)/255f,1);tc4revival.client.LegacyGui.blit(g,a.getImage(),leftPos+118,topPos+41,0,0,16,16,16,16);g.setColor(1,1,1,1);}com.mojang.blaze3d.systems.RenderSystem.disableBlend();}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public boolean mouseClicked(double x,double y,int button){double rx=x-leftPos,ry=y-topPos;if(button==0&&ry>12&&ry<24&&((rx>106&&rx<117)||(rx>59&&rx<70))){int cut=1-menu.tile.cut;menu.tile.cut=cut;minecraft.gameMode.handleInventoryButtonClick(menu.containerId,cut);return true;}return super.mouseClicked(x,y,button);}
 @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
