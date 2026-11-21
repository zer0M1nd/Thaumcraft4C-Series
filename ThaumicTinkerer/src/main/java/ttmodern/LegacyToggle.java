package ttmodern;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Thirteen-pixel atlas checkbox used by the original tablet, magnet and warp gate. */
public class LegacyToggle extends Button {
    private final ResourceLocation texture;
    private final BooleanSupplier selected;
    public LegacyToggle(int x,int y,String texture,Component label,BooleanSupplier selected,Runnable action){
        super(x,y,13,13,label,b->action.run(),DEFAULT_NARRATION);
        this.texture=ResourceLocation.parse(Tinkerer.ID+":textures/gui/"+texture+".png");this.selected=selected;
    }
    @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){tc4revival.client.LegacyGui.blit(g,texture,getX(),getY(),176,selected.getAsBoolean()?13:0,13,13);}
}
