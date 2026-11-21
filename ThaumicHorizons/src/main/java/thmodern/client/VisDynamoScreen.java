package thmodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.aspect.TCAspects;
import thmodern.node.VisDynamoMenu;

public class VisDynamoScreen extends AbstractContainerScreen<VisDynamoMenu>{
    private int flashTimer,flashX,flashY,flashColor;
    public VisDynamoScreen(VisDynamoMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=111;imageHeight=104;}
    private int x(int i){return i%2==0?11:83;}
    private int y(int i){return i==1?11:12+i/2*33;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.fromNamespaceAndPath("thaumichorizons","textures/gui/guidynamo.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        for(int i=0;i<6;i++){
            var aspect=TCAspects.catalog().require(VisDynamoMenu.ASPECTS.get(i));
            int color=aspect.color();
            // Legacy UtilsFX.drawTag(bw=true) tints only the icon, preserving its alpha.
            if(menu.enabled(i))graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            else graphics.setColor(.1f,.1f,.1f,.8f);
            tc4revival.client.LegacyGui.blit(graphics,aspect.texture(),leftPos+x(i),topPos+y(i),0,0,16,16,16,16);
            graphics.setColor(1,1,1,1);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){
        if(flashTimer<=0)return;
        // The original animation advances once per rendered frame, with rotated atlas UVs.
        int part=--flashTimer;
        float u=(.5f+part/8f)%1f,v=.5f,size=.0624375f;
        graphics.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,ResourceLocation.parse("thaumcraft:textures/misc/particles.png"));
        var buffer=com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        var pose=graphics.pose().last().pose();
        int r=flashColor>>16&255,g=flashColor>>8&255,b=flashColor&255;
        buffer.vertex(pose,flashX,flashY+16,0).uv(u+size,v+size).color(r,g,b,255).endVertex();
        buffer.vertex(pose,flashX+16,flashY+16,0).uv(u+size,v).color(r,g,b,255).endVertex();
        buffer.vertex(pose,flashX+16,flashY,0).uv(u,v).color(r,g,b,255).endVertex();
        buffer.vertex(pose,flashX,flashY,0).uv(u,v+size).color(r,g,b,255).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);
        for(int i=0;i<6;i++)if(isHovering(x(i),y(i),16,16,mouseX,mouseY))graphics.renderTooltip(font,Component.literal(thaumcraft.api.aspects.AspectViews.display(TCAspects.catalog().require(VisDynamoMenu.ASPECTS.get(i))).displayName()),mouseX,mouseY);
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button==0)for(int i=0;i<6;i++)if(isHovering(x(i),y(i),16,16,mouseX,mouseY)&&minecraft.gameMode!=null){
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i);
            flashTimer=8;flashX=(int)mouseX-leftPos-8;flashY=(int)mouseY-topPos-8;
            flashColor=thaumcraft.api.aspects.AspectViews.display(TCAspects.catalog().require(VisDynamoMenu.ASPECTS.get(i))).color();
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(tc4revival.content.TCSounds.HH_OFF.get(),1f+minecraft.player.getRandom().nextFloat()*.1f,.2f));
            return true;
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
}
