package tc4revival.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.ArcaneSpaMenu;
public  class ArcaneSpaScreen extends AbstractContainerScreen<ArcaneSpaMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_spa.png");
    public ArcaneSpaScreen(ArcaneSpaMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);LegacyGui.blit(g,TEXTURE,leftPos+89,topPos+35,208,menu.spa().mix()?16:32,8,8);
        var fluid=menu.spa().tank().getFluid();
        if(!fluid.isEmpty()) {
            var extension=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
            var sprite=minecraft.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(extension.getStillTexture(fluid));int color=extension.getTintColor(fluid);
            g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            for(int i=0;i<6;i++)LegacyGui.blit(g,leftPos+107,topPos+15+i*8,0,8,8,sprite);g.setColor(1,1,1,1);
            LegacyGui.blit(g,TEXTURE,leftPos+107,topPos+15,107,15,10,48-48*fluid.getAmount()/5000);
        }
        LegacyGui.blit(g,TEXTURE,leftPos+106,topPos+11,232,0,10,55);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);
        if(x>=leftPos+104&&x<leftPos+114&&y>=topPos+10&&y<topPos+65&&!menu.spa().tank().isEmpty())g.renderComponentTooltip(font,java.util.List.of(menu.spa().tank().getFluid().getDisplayName(),Component.literal(menu.spa().tank().getFluidAmount()+" mb")),x,y);
        if(x>=leftPos+88&&x<leftPos+98&&y>=topPos+34&&y<topPos+44)g.renderTooltip(font,Component.translatable("text.spa.mix."+menu.spa().mix()),x,y);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(x>=leftPos+89&&x<leftPos+97&&y>=topPos+35&&y<topPos+43&&minecraft.gameMode!=null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(tc4revival.content.TCSounds.CAMERA_CLACK.get(),1,.4f));return true;
        }
        return super.mouseClicked(x,y,button);
    }
}
