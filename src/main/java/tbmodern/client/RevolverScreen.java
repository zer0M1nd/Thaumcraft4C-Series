package tbmodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tbmodern.Revolver;

public final class RevolverScreen extends AbstractContainerScreen<Revolver.Menu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("thaumicbases","textures/gui/revolver.png");
    public RevolverScreen(Revolver.Menu menu, Inventory inventory, Component title){super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){tc4revival.client.LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){tc4revival.client.LegacyGui.blit(graphics,TEXTURE,8+menu.gunSlot*18,142,240,0,16,16);}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);}
}
