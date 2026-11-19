package thaumicomniverse.client;

import net.minecraft.client.gui.GuiGraphics;
import net.p3pp3rf1y.sophisticatedcore.client.gui.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import thaumicomniverse.compat.backpack.EssentiaTankContainer;

/** Adapter for Sophisticated Core's original inventory-part API. */
public final class EssentiaTankInventoryPart extends UpgradeInventoryPartBase<EssentiaTankContainer> {
    private final EssentiaTankView view;
    public EssentiaTankInventoryPart(int slot, EssentiaTankContainer container, Position pos, int height, StorageScreenBase<?> screen) {
        super(slot, container);
        view=new EssentiaTankView(container,pos,height,screen);
    }
    @Override public void render(GuiGraphics g,int x,int y){view.render(g,x,y);}
    @Override public boolean handleMouseReleased(double x,double y,int button){return false;}
    @Override public void renderErrorOverlay(GuiGraphics g){view.renderErrorOverlay(g);}
    @Override public void renderTooltip(StorageScreenBase<?> screen,GuiGraphics g,int x,int y){view.renderTooltip(screen,g,x,y);}
}
