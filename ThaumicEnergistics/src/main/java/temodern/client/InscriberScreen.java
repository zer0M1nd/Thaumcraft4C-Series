package temodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import temodern.crafting.*;

/** Original 210x244 inscriber, including its animated window and confirmed-save particles. */
public final class InscriberScreen extends AbstractContainerScreen<InscriberMenu> {
    private static final ResourceLocation GUI=ResourceLocation.parse("thaumicenergistics:textures/gui/knowledge.inscriber.png");
    private static final ResourceLocation BACKGROUND=ResourceLocation.parse("thaumicenergistics:textures/research/background.png");
    private static final ResourceLocation PARTICLES=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private int revision=-1;
    private long savedAt=-10000;
    public InscriberScreen(InscriberMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=210;imageHeight=244;titleLabelX=6;titleLabelY=6;}
    private ItemStack output(int index){var patterns=menu.patterns();if(index>=patterns.size())return ItemStack.EMPTY;var pattern=new ArcanePattern.Decoder().decodePattern(patterns.get(index),minecraft.level,false);return pattern==null?ItemStack.EMPTY:((appeng.api.stacks.AEItemKey)pattern.getPrimaryOutput().what()).toStack();}
    private void blit(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int w,int h){tc4revival.client.LegacyGui.blit(g,texture,leftPos+x,topPos+y,u,v,w,h);}
    private boolean at(double x,double y,int px,int py,int w,int h){return x>=leftPos+px&&x<leftPos+px+w&&y>=topPos+py&&y<topPos+py+h;}
    private String hint(){return "thaumicenergistics.tooltip.button.inscriber."+switch(menu.saveState()){case 0->"missing";case 1->"invalid";case 2->"full";case 4->"delete";default->"save";};}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        long slow=net.minecraft.Util.getMillis()/100;float red=Math.abs(1-(slow%50)/25f);
        g.setColor(red,1-red,1,1);blit(g,BACKGROUND,80,106,55-(int)(slow%10),25,35,20);
        g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        blit(g,GUI,0,0,0,0,imageWidth,imageHeight);
        for(int i=0;i<21;i++){int x=26+i%7*18,y=18+i/7*18;var stack=output(i);g.renderItem(stack,leftPos+x,topPos+y);g.renderItemDecorations(font,stack,leftPos+x,topPos+y);}
        var icon=switch(menu.saveState()){case 3->appeng.client.gui.Icon.ACCESS_WRITE;case 4->appeng.client.gui.Icon.CLEAR;default->appeng.client.gui.Icon.INVALID;};
        float brightness=menu.saveState()<3?.4f:at(mx,my,141,109,16,16)?1:.85f;g.setColor(brightness,brightness,brightness,1);
        blit(g,appeng.client.gui.Icon.TEXTURE,141,109,240,240,16,16);blit(g,appeng.client.gui.Icon.TEXTURE,141,109,icon.x,icon.y,16,16);
        g.setColor(1,1,1,1);g.blit(appeng.client.gui.Icon.TEXTURE,leftPos+80,topPos+89,8,8,appeng.client.gui.Icon.CLEAR.x,appeng.client.gui.Icon.CLEAR.y,16,16,256,256);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,6,6,0x404040,false);
        if(revision!=menu.saveRevision()){if(revision>=0)savedAt=net.minecraft.Util.getMillis();revision=menu.saveRevision();}
        long now=net.minecraft.Util.getMillis();
        if(now-savedAt>850)return;
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        g.setColor(.2f,.5f,1,.9f);
        for(int i=0;i<9;i++){double progress=(now-savedAt-i*50)/400d;if(progress<0||progress>1)continue;int sx=26+i%3*18,sy=90+i/3*18;
            tc4revival.client.LegacyGui.blit(g,PARTICLES,(int)(sx+(186-sx)*progress),(int)(sy+(8-sy)*progress),(int)(now/1000%4)*16,64,16,16);}
        g.flush();g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){
            if(at(x,y,141,109,16,16)){if(menu.saveState()>=3)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true;}
            if(at(x,y,80,89,8,8)){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,22);return true;}
            int cx=(int)x-leftPos-26,cy=(int)y-topPos-18;if(cx>=0&&cx<126&&cy>=0&&cy<54){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1+cx/18+cy/18*7);return true;}
        }return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);
        if(at(x,y,141,109,16,16))g.renderTooltip(font,Component.translatable(hint()),x,y);
        else if(at(x,y,80,89,8,8))g.renderTooltip(font,Component.translatable("gui.thaumicenergistics.clear_grid"),x,y);
        int cx=x-leftPos-26,cy=y-topPos-18;if(cx>=0&&cx<126&&cy>=0&&cy<54){var stack=output(cx/18+cy/18*7);if(!stack.isEmpty())g.renderTooltip(font,stack,x,y);}
    }
}
