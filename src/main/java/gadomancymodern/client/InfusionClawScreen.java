package gadomancymodern.client;
import gadomancymodern.claw.InfusionClawMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import tc4revival.client.LegacyGui;
public class InfusionClawScreen extends AbstractContainerScreen<InfusionClawMenu> {
    protected static final ResourceLocation TEXTURE=ResourceLocation.parse("gadomancy:textures/gui/gui_infusion_claw.png");
    public InfusionClawScreen(InfusionClawMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mx,int my){LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);LegacyGui.blit(graphics,TEXTURE,leftPos+114,topPos+36,244,19,8,8);if(menu.claw.locked())LegacyGui.blit(graphics,TEXTURE,leftPos+114,topPos+36,244,27,8,8);graphics.pose().pushPose();graphics.pose().translate(leftPos+124,topPos+38,0);graphics.pose().scale(.5f,.5f,1);graphics.drawString(font,Component.translatable("gadomancy.info.InfusionClaw.lock"),0,0,menu.claw.locked()?0xffffff:0x808080,false);graphics.pose().popPose();}
    @Override protected void renderLabels(GuiGraphics graphics,int mx,int my){if(menu.claw.isRemoved()){minecraft.player.closeContainer();return;}if(menu.claw.running()||!menu.getCarried().isEmpty()&&!menu.claw.canPlaceItem(0,menu.getCarried())){graphics.pose().pushPose();graphics.pose().translate(0,0,200);graphics.setColor(1,1,1,.5f);LegacyGui.blit(graphics,TEXTURE,80,32,240,0,16,16);graphics.setColor(1,1,1,1);graphics.pose().popPose();}}
    @Override public void render(GuiGraphics graphics,int mx,int my,float partial){renderBackground(graphics);super.render(graphics,mx,my,partial);renderTooltip(graphics,mx,my);}
    @Override public boolean mouseClicked(double x,double y,int button){if(x>=leftPos+114&&x<leftPos+122&&y>=topPos+36&&y<topPos+44){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,menu.claw.locked()?0:1);menu.claw.locked(!menu.claw.locked());return true;}return super.mouseClicked(x,y,button);}
}
