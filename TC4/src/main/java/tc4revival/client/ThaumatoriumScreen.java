package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.ThaumatoriumMenu;

public  class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/gui/gui_thaumatorium.png");
    private int index,startAspect,lastSize=-1;
    public ThaumatoriumScreen(ThaumatoriumMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.setColor(1,1,1,1); LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var choices=menu.choices(); var process=menu.process(); if (choices.isEmpty() || process==null) return;
        if (lastSize!=choices.size()) {
            lastSize=choices.size(); startAspect=0;
            for (int i=0;i<choices.size();i++) if (process.selected().contains(choices.get(i).getId())) { index=i; break; }
        }
        index=Math.max(0,Math.min(index,choices.size()-1)); var recipe=choices.get(index);
        var costs=recipe.costs(); var aspects=costs.aspects().stream().sorted(java.util.Comparator.comparing(tc4revival.aspect.Aspect::tag)).toList();
        if (aspects.size()<=6) startAspect=0; else startAspect=Math.min(startAspect,aspects.size()-1);
        if (choices.size()>1) {
            LegacyGui.blit(g,TEXTURE,leftPos+128,topPos+16,index>0 ? 192 : 176,16,16,8);
            LegacyGui.blit(g,TEXTURE,leftPos+128,topPos+24,index<choices.size()-1 ? 192 : 176,24,16,8);
        }
        if (aspects.size()>6) {
            LegacyGui.blit(g,TEXTURE,leftPos+32,topPos+40,startAspect>0 ? 192 : 176,32,8,16);
            LegacyGui.blit(g,TEXTURE,leftPos+136,topPos+40,startAspect<aspects.size()-1 ? 200 : 184,32,8,16);
        }
        boolean selected=process.chaotic() || process.selected().contains(recipe.getId());
        if (!process.selected().isEmpty()) {
            if (selected || inside(mx,my,112,16,16,16)) LegacyGui.blit(g,TEXTURE,leftPos+104,topPos+8,176,96,48,48);
            float alpha=.6f+(float)Math.sin(minecraft.player.tickCount/5f)*.4f+.4f;
            g.setColor(1,1,1,Math.min(1,alpha)); LegacyGui.blit(g,TEXTURE,leftPos+88,topPos+16,176,56,24,24); g.setColor(1,1,1,1);
        }
        var stored=process.stored(); int position=0;
        for (int i=startAspect;i<Math.min(aspects.size(),startAspect+6);i++,position++) {
            var aspect=aspects.get(i); int x=leftPos+40+16*position;
            int tint=thaumcraft.api.client.ClientAspectViews.view(aspect).color();
            g.setColor((tint>>16&255)/255f,(tint>>8&255)/255f,(tint&255)/255f,1);
            LegacyGui.blit(g,thaumcraft.api.client.ClientAspectViews.view(aspect).texture(),x,topPos+40,0,0,16,16,16,16);
            g.setColor(1,1,1,1);
            String amount=Integer.toString(costs.amount(aspect));
            g.pose().pushPose(); g.pose().translate(x+16,topPos+51,200); g.pose().scale(.5f,.5f,1);
            g.drawString(font,amount,-font.width(amount),0,0xFFFFFF,true); g.pose().popPose();
            if (selected) {
                LegacyGui.blit(g,TEXTURE,x+1,topPos+57,176,8,14,6);
                int color=thaumcraft.api.client.ClientAspectViews.view(aspect).color(); g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
                int width=Math.min(12,(int)(stored.amount(aspect)/(float)costs.amount(aspect)*12));
                LegacyGui.blit(g,TEXTURE,x+2,topPos+58,176,0,width,4); g.setColor(1,1,1,1);
            }
        }
        var output=recipe.getResultItem(minecraft.level.registryAccess());
        boolean dull=!process.chaotic() && (process.selected().size()<process.limit() || selected);
        if (dull) g.setColor(.5f,.5f,.5f,.6f+(float)Math.sin(minecraft.player.tickCount/4f)*.3f);
        g.renderItem(output,leftPos+112,topPos+16); g.renderItemDecorations(font,output,leftPos+112,topPos+16);
        g.setColor(1,1,1,1);
        if (process.limit()>1) {
            String capacity=process.selected().size()+"/"+process.limit();
            g.pose().pushPose(); g.pose().translate(leftPos+136,topPos+33,0); g.pose().scale(.5f,.5f,1);
            g.drawString(font,capacity,-font.width(capacity)/2,0,0xFFFFFF,false); g.pose().popPose();
        }
    }
    private boolean inside(double x,double y,int left,int top,int width,int height) {
        return x>=leftPos+left && y>=topPos+top && x<leftPos+left+width && y<topPos+top+height;
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        var choices=menu.choices(); var process=menu.process();
        if (process!=null && !choices.isEmpty() && index>=0 && index<choices.size()) {
            var recipe=choices.get(index); boolean changed=false,select=false;
            if (!process.chaotic() && inside(x,y,112,16,16,16) && (process.selected().size()<process.limit() || process.selected().contains(recipe.getId()))) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,index); changed=true; select=true;
            } else if (index>0 && inside(x,y,128,16,16,8)) { index--; changed=true; }
            else if (index<choices.size()-1 && inside(x,y,128,24,16,8)) { index++; changed=true; }
            else if (recipe.costs().size()>6 && startAspect>0 && inside(x,y,32,40,8,16)) { startAspect--; changed=true; }
            else if (recipe.costs().size()>6 && startAspect<recipe.costs().size()-1 && inside(x,y,136,40,8,16)) { startAspect++; changed=true; }
            if (changed) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        select ? tc4revival.content.TCSounds.HH_ON.get() : tc4revival.content.TCSounds.CAMERA_CLACK.get(),1,select ? .3f : .4f)); return true;
            }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        renderBackground(g); super.render(g,x,y,partial); renderTooltip(g,x,y);
        var choices=menu.choices();
        if (index>=0 && index<choices.size() && inside(x,y,112,16,16,16)) g.renderTooltip(font,choices.get(index).getResultItem(minecraft.level.registryAccess()),x,y);
    }
}
