package automagy.client;

import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Original compact controls accept both mouse buttons and use the atlas checkbox cells. */
public class LegacyControls {
    public static ResourceLocation texture(String name){return ResourceLocation.parse("automagy:textures/gui/"+name+".png");}
    public static void text(GuiGraphics g,Component text,int x,int y,float scale,int color){
        text(g,text,x,y,scale,color,false);
    }
    public static void text(GuiGraphics g,Component text,int x,int y,float scale,int color,boolean shadow){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.drawString(Minecraft.getInstance().font,text,0,0,color,shadow);g.pose().popPose();
    }
    public static void centered(GuiGraphics g,Component text,int x,int y,float scale,int color){text(g,text,x-(int)(Minecraft.getInstance().font.width(text)*scale/2),y,scale,color);}
    public static void centered(GuiGraphics g,Component text,int x,int y,float scale,int color,boolean shadow){text(g,text,x-(int)(Minecraft.getInstance().font.width(text)*scale/2),y,scale,color,shadow);}
    public static Button small(int x,int y,int w,int h,Supplier<Component> text,Consumer<Boolean> action){
        return small(x,y,w,h,.75f,text,action);
    }
    public static Button small(int x,int y,int w,int h,float fontScale,Supplier<Component> text,Consumer<Boolean> action){
        return new Button(x,y,w,h,Component.empty(),b->{},narration->narration.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                super.renderWidget(g,mx,my,partial);var label=text.get();float scale=Math.min(fontScale,(width-4f)/Math.max(1,Minecraft.getInstance().font.width(label)));
                LegacyControls.text(g,label,getX()+(int)((width-Minecraft.getInstance().font.width(label)*scale)/2),getY()+(int)((height-8*scale)/2),scale,active?0xffffff:0x888888);
            }
            @Override public boolean mouseClicked(double mx,double my,int button){if(active&&visible&&(button==0||button==1)&&isMouseOver(mx,my)){playDownSound(Minecraft.getInstance().getSoundManager());action.accept(button==1);return true;}return false;}
            @Override public void onPress(){action.accept(false);}
        };
    }
    public static Button checkbox(int x,int y,ResourceLocation texture,BooleanSupplier selected,Consumer<Boolean> action){
        return new Button(x,y,8,8,Component.empty(),b->action.accept(false),narration->narration.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){tc4revival.client.LegacyGui.blit(g,texture,getX(),getY(),176,selected.getAsBoolean()?8:0,8,8);}
            @Override public boolean mouseClicked(double mx,double my,int button){if(active&&visible&&(button==0||button==1)&&isMouseOver(mx,my)){playDownSound(Minecraft.getInstance().getSoundManager());action.accept(button==1);return true;}return false;}
        };
    }
    public static Button recolored(int x,int y,int w,int h,float scale,Component label,Runnable action){
        return new Button(x,y,w,h,label,b->action.run(),narration->narration.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                int v=46+(!active?0:isHovered()?2:1)*20;
                var atlas=texture("widgets_recolor");
                tc4revival.client.LegacyGui.blit(g,atlas,getX(),getY(),0,v,width/2,height/2);
                tc4revival.client.LegacyGui.blit(g,atlas,getX()+width/2,getY(),200-width/2,v,width/2,height/2);
                tc4revival.client.LegacyGui.blit(g,atlas,getX(),getY()+height/2,0,v+20-height/2,width/2,height/2);
                tc4revival.client.LegacyGui.blit(g,atlas,getX()+width/2,getY()+height/2,200-width/2,v+20-height/2,width/2,height/2);
                centered(g,getMessage(),getX()+width/2,getY()+(int)((height-8*scale)/2),scale,!active?0xa0a0a0:isHovered()?0xffffa0:0xe0e0e0,true);
            }
        };
    }
}
