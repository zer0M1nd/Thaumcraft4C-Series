package thaumicfutures.devices.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import thaumicfutures.devices.*;

public class DioptraScreen extends Screen {
    private final Dioptra tile;
    private float yaw=.6f,tilt=.65f,zoom=1;
    private int selected=-1;
    private record Point(NodeReading node,float x,float y,float depth,int index) {}
    public DioptraScreen(Dioptra tile) { super(Component.translatable("block.thaumicfutures.dioptra"));this.tile=tile; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width-84,height-26,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("<"),b->select(-1)).bounds(12,height-26,24,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->select(1)).bounds(40,height-26,24,20).build());
        addRenderableWidget(Button.builder(Component.translatable("thaumicfutures.dioptra.reset"),b->{yaw=.6f;tilt=.65f;zoom=1;selected=-1;}).bounds(70,height-26,80,20).build());
    }
    private void select(int step) { if(!tile.nodes().isEmpty())selected=Math.floorMod(selected+step,tile.nodes().size()); }
    @Override public void tick() { if(tile.isRemoved() || minecraft.level!=tile.getLevel() || minecraft.player==null || minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(tile.getBlockPos()))>64)onClose(); }
    private Point project(NodeReading node,int index) {
        float dx=node.pos().getX()-tile.getBlockPos().getX(),dz=node.pos().getZ()-tile.getBlockPos().getZ(),dy=node.pos().getY()-tile.getBlockPos().getY();
        float x=dx*Mth.cos(yaw)-dz*Mth.sin(yaw),z=dx*Mth.sin(yaw)+dz*Mth.cos(yaw);
        float scale=Math.min((width-190)/260f,(height-100)/220f)*zoom;
        return new Point(node,(width-170)/2f+x*scale,height/2f+(z*Mth.sin(tilt)-dy*Mth.cos(tilt))*scale,z*Mth.cos(tilt)+dy*Mth.sin(tilt),index);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);g.fill(7,30,width-174,height-34,0xe8101420);g.fill(width-168,30,width-8,height-34,0xe81c1b26);
        g.drawCenteredString(font,title,width/2,10,0xffe9c6);
        g.drawString(font,Component.translatable("thaumicfutures.dioptra.count",tile.nodes().size(),tile.surveyedChunks()),14,36,0xe0d8ee,false);
        var points=new ArrayList<Point>();for(int i=0;i<tile.nodes().size();i++)points.add(project(tile.nodes().get(i),i));points.sort(Comparator.comparingDouble(Point::depth));
        float time=minecraft.level.getGameTime()+partial;Point hovered=null;double nearest=144;
        g.enableScissor(8,50,width-175,height-35);
        int ox=(width-170)/2,oy=height/2;g.fill(ox-3,oy-1,ox+4,oy+2,0xffffc66b);g.fill(ox-1,oy-3,ox+2,oy+4,0xffffc66b);
        for(var p:points) {
            NodeGlyph.screen(p.node,time,g,p.x,p.y,6.5f);
            if(p.index==selected)g.renderOutline((int)p.x-11,(int)p.y-11,23,23,0xffeecb7c);
            double distance=(p.x-mouseX)*(p.x-mouseX)+(p.y-mouseY)*(p.y-mouseY);
            if(distance<nearest && mouseX<width-174 && mouseY>50 && mouseY<height-35) { nearest=distance;hovered=p; }
        }
        g.disableScissor();
        var chosen=hovered!=null?hovered.node:selected>=0 && selected<tile.nodes().size()?tile.nodes().get(selected):null;
        int x=width-160,y=40;
        if(chosen!=null) {
            g.drawString(font,Component.literal(chosen.pos().getX()+", "+chosen.pos().getY()+", "+chosen.pos().getZ()),x,y,0xffefcf,false);y+=16;
            g.drawWordWrap(font,Component.translatable(chosen.live()?"thaumicfutures.dioptra.live":"thaumicfutures.dioptra.saved"),x,y,144,0xbbb3c8);y+=28;
            g.drawString(font,Component.translatable("nodetype."+chosen.type()+".name"),x,y,0xffffff,false);y+=16;
            if(chosen.modifier()!=null) { g.drawString(font,Component.translatable("nodemod."+chosen.modifier()+".name"),x,y,0xcccccc,false);y+=16; }
            for(var aspect:chosen.capacity().aspects()) {
                var line=Component.translatable("tc.aspect."+aspect.tag()).append(" "+chosen.charge().amount(aspect)+" / "+chosen.capacity().amount(aspect));
                int textHeight=font.split(line,144).size()*font.lineHeight;
                if(y+textHeight>height-42)break;
                int color=aspect.color();
                // Dark aspects still need readable labels on the dark instrument panel.
                color=0xff000000 | (((color>>16&255)+255)/2<<16) | (((color>>8&255)+255)/2<<8) | ((color&255)+255)/2;
                g.drawWordWrap(font,line,x,y,144,color);y+=textHeight+4;
            }
        } else g.drawWordWrap(font,Component.translatable(tile.active()?"thaumicfutures.dioptra.inspect":"thaumicfutures.dioptra.inactive"),x,y,144,0xd9cfe3);
        g.drawWordWrap(font,Component.translatable("thaumicfutures.dioptra.controls"),160,height-25,Math.max(80,width-254),0xb8a9c8);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(button==0 && x<width-174 && y>30 && y<height-34) { yaw+=dx*.012f;tilt=Mth.clamp(tilt+(float)dy*.01f,-1.4f,1.4f);return true; }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) { zoom=Mth.clamp(zoom*(float)Math.pow(1.15,delta),.25f,8);return true; }
}
