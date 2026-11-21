package thaumicomniverse.client;

import net.minecraft.client.gui.GuiGraphics;
import net.p3pp3rf1y.sophisticatedcore.client.gui.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import thaumicomniverse.compat.backpack.EssentiaTankContainer;

/** Loaded only when Sophisticated Core supplies the replacement control API. */
public final class ModernBackpackClient {
    public static void register() {
        UpgradeGuiManager.registerInventoryControl(EssentiaTankContainer.TYPE, TankControl::new);
    }
    private static final class TankControl extends UpgradeInventoryControlBase {
        private final EssentiaTankView view;
        TankControl(int slot, EssentiaTankContainer container, Position pos, int height, StorageScreenBase<?> screen) {
            view=new EssentiaTankView(container,pos,height,screen);
        }
        @Override public void render(GuiGraphics g,int x,int y){view.render(g,x,y);}
        @Override public boolean handleMouseReleased(double x,double y,int button){return false;}
        @Override public void renderErrorOverlay(GuiGraphics g){view.renderErrorOverlay(g);}
        @Override public void renderTooltip(StorageScreenBase<?> screen,GuiGraphics g,int x,int y){view.renderTooltip(screen,g,x,y);}
    }
}
