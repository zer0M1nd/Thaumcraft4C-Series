package technomancy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import technomancy.*;

public class RitualTomeScreen extends Screen {
    private int page;
    public RitualTomeScreen(){super(Component.translatable("item.technomancy.ritual_tome"));}
    @Override protected void init(){
        int left=(width-256)/2,top=(height-256)/2;
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.floorMod(page-1,18);}).bounds(left+19,top+230,22,16).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->{page=(page+1)%18;}).bounds(left+215,top+230,22,16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(left+100,top+230,56,16).build());
    }
    @Override public boolean isPauseScreen(){return false;}
    private static String picture(String id) {
        if(id.equals("fountain"))return "fountain";
        if(id.equals("extraction"))return "extract";
        int tier=id.charAt(id.length()-1)-'0';String family=id.substring(0,id.length()-2);
        return switch(family){case "black_hole"->"blackhole";case "cave_in"->"cavein";default->family;}+"t"+tier;
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);int left=(width-256)/2,top=(height-256)/2;
        g.blit(Technomancy.id("textures/gui/tomebackritual.png"),left,top,0,0,256,256,256,256);
        String key=page==0?"basics":page==1?"existence":RitualLogic.definitions().get(page-2).id();
        g.drawString(font,Component.translatable("ritual.technomancy."+key),left+128-font.width(Component.translatable("ritual.technomancy."+key))/2,top+16,0x342b1d,false);
        int y=top+36;
        for(var line:font.split(Component.translatable("ritual.technomancy."+key+".description"),192)){g.drawString(font,line,left+32,y,0x342b1d,false);y+=11;}
        if(page>=2){
            var d=RitualLogic.definitions().get(page-2);
            g.pose().pushPose();
            g.pose().translate(left+68,top+90,0);g.pose().scale(3.75f,3.75f,1);
            g.blit(Technomancy.id("textures/gui/"+picture(key)+".png"),0,0,0,0,32,32,32,32);
            g.pose().popPose();
        }
        String pageText=(page+1)+" / 18";
        g.drawString(font,pageText,left+128-font.width(pageText)/2,top+215,0x342b1d,false);
        super.render(g,mouseX,mouseY,partial);
    }
}
