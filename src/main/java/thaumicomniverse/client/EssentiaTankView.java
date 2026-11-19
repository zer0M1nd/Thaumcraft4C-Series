package thaumicomniverse.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import thaumicomniverse.compat.backpack.EssentiaTankContainer;

/** Uses Sophisticated Core's native tank frame in the columns reserved by the upgrade. */
public final class EssentiaTankView {
    private final EssentiaTankContainer container;
    private static final TextureBlitData OVERLAY=new TextureBlitData(GuiHelper.GUI_CONTROLS,Dimension.SQUARE_256,new UV(47,30),new Dimension(16,18));
    private final Position pos;
    private final int height;
    private final StorageScreenBase<?> screen;
    public EssentiaTankView(EssentiaTankContainer container,Position pos,int height,StorageScreenBase<?> screen){
        this.container=container;this.pos=pos;this.height=height;this.screen=screen;
    }
    public void render(GuiGraphics g,int mouseX,int mouseY){
        int x=pos.x()+9,y=pos.y(),cap=Math.min(18,height/2);
        GuiHelper.blit(g,x,y,GuiHelper.BAR_BACKGROUND_TOP,18,cap);
        for(int offset=cap;offset<height-cap;offset+=18)
            GuiHelper.blit(g,x,y+offset,GuiHelper.BAR_BACKGROUND_MIDDLE,18,Math.min(18,height-cap-offset));
        GuiHelper.blit(g,x,y+height-cap,GuiHelper.BAR_BACKGROUND_BOTTOM,18,cap);
        var aspect=container.aspect();
        if(aspect!=null&&container.amount()>0){
            int fill=(int)((height-2L)*Math.min(container.amount(),container.capacity())/Math.max(1,container.capacity()));
            var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/animatedglow"));
            GuiHelper.renderTiledFluidTextureAtlas(g,sprite,0xff000000|aspect.color(),x+1,y+height-1-fill,fill);
        }
        g.pose().pushPose();g.pose().translate(0,0,100);
        for(int offset=0;offset<height;offset+=18)GuiHelper.blit(g,x+1,y+offset,OVERLAY,16,Math.min(18,height-offset));
        g.pose().popPose();
    }
    public boolean handleMouseReleased(double mouseX,double mouseY,int button){return false;}
    public void renderErrorOverlay(GuiGraphics g){screen.renderOverlay(g,StorageScreenBase.ERROR_SLOT_COLOR,pos.x()+10,pos.y()+1,16,height-2);}
    public void renderTooltip(StorageScreenBase<?> screen,GuiGraphics g,int mouseX,int mouseY){
        int x=screen.getGuiLeft()+pos.x()+10,y=screen.getGuiTop()+pos.y()+1;
        if(mouseX<x||mouseX>=x+16||mouseY<y||mouseY>=y+height-2)return;
        var lines=new ArrayList<Component>();
        lines.add(Component.translatable("item.thaumicomniverse.essentia_tank_upgrade"));
        if(container.aspect()!=null)lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(container.aspect()).displayName()));
        lines.add(Component.literal(container.amount()+" / "+container.capacity()));
        g.renderTooltip(Minecraft.getInstance().font,lines,Optional.empty(),mouseX,mouseY);
    }
}
