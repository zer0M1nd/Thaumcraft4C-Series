package automagy.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Old Automagy text fields use a 0.75 GUI scale for both drawing and mouse input. */
public class LegacyEditBox extends EditBox {
    public LegacyEditBox(Font font,int x,int y,int width,int height,Component label) {
        super(font,x,y,width,height,label);
    }

    @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        g.pose().pushPose();
        g.pose().translate(getX()*.25f,getY()*.25f,0);
        g.pose().scale(.75f,.75f,1);
        super.renderWidget(g,(int)localX(mx),(int)localY(my),partial);
        g.pose().popPose();
    }

    private double localX(double x) { return getX()+(x-getX())/.75; }
    private double localY(double y) { return getY()+(y-getY())/.75; }

    @Override public boolean mouseClicked(double x,double y,int button) {
        return super.mouseClicked(localX(x),localY(y),button);
    }

    @Override public boolean isMouseOver(double x,double y) {
        return visible&&x>=getX()&&x<getX()+getWidth()*.75&&y>=getY()&&y<getY()+getHeight()*.75;
    }
}
