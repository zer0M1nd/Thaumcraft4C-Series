package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;

/** Small parchment controls, using the book's brown/gold palette rather than gray vanilla buttons. */
public  class ResearchWidgets {
    private ResearchWidgets() {}
    public static void smallButton(GuiGraphics g,int x,int y,int width,int height,boolean highlighted) {
        var texture=net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/gui/research_button.png");
        g.flush();float tint=highlighted?1f:.8f;g.setColor(tint,tint,tint,1);
        int[] source={0,2,6},size={2,4,2},dx={0,2,width-2},dy={0,2,height-2};
        int[] targetW={2,width-4,2},targetH={2,height-4,2};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)
            LegacyGui.blit(g,texture,x+dx[col],y+dy[row],targetW[col],targetH[row],source[col],source[row],size[col],size[row],8,8);
        g.setColor(1,1,1,1);
    }
    public static void panel(GuiGraphics g,int x,int y,int width,int height,boolean highlighted) {
        g.fill(x,y,x+width,y+height,0xff392a1c);
        g.fill(x+1,y+1,x+width-1,y+height-1,highlighted?0xffb99a5f:0xff90713f);
        g.fill(x+2,y+2,x+width-2,y+height-2,highlighted?0xffe2d0a3:0xffcdb98d);
        g.fill(x+3,y+3,x+width-3,y+4,0xffeadbb6);
    }
}
