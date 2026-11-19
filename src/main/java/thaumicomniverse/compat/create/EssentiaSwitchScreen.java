package thaumicomniverse.compat.create;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.redstone.thresholdSwitch.ConfigureThresholdSwitchPacket;
import com.simibubi.create.foundation.gui.*;
import com.simibubi.create.foundation.gui.widget.*;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Thresholds remain settings even while the observed vessel or its client update is absent. */
public class EssentiaSwitchScreen extends AbstractSimiScreen {
    private final EssentiaSwitch.Tile tile;
    private ScrollInput above,below;
    private boolean inverted,dirty,initializing;
    private final AllGuiTextures background=AllGuiTextures.THRESHOLD_SWITCH;
    public EssentiaSwitchScreen(EssentiaSwitch.Tile tile){super(Component.translatable("block.thaumicomniverse.essentia_stockpile_switch"));this.tile=tile;inverted=tile.isInverted();}
    @Override protected void init(){
        setWindowSize(background.getWidth(),background.getHeight());setWindowOffset(-20,0);super.init();initializing=true;
        above=new ScrollInput(guiLeft+48,guiTop+23,103,18).withRange(1,Integer.MAX_VALUE).withStepFunction(c->c.shift?10:1)
            .titled(Component.translatable("create.gui.threshold_switch.upper_threshold")).setState(Math.max(1,tile.onWhenAbove));
        below=new ScrollInput(guiLeft+48,guiTop+47,103,18).withRange(0,Integer.MAX_VALUE-1).withStepFunction(c->c.shift?10:1)
            .titled(Component.translatable("create.gui.threshold_switch.lower_threshold")).setState(Math.max(0,tile.offWhenBelow));
        above.calling(value->{if(below.getState()>=value)below.setState(value-1);if(!initializing)dirty=true;});
        below.calling(value->{if(above.getState()<=value)above.setState(value+1);if(!initializing)dirty=true;});
        addRenderableWidget(above);addRenderableWidget(below);
        var confirm=new IconButton(guiLeft+background.getWidth()-33,guiTop+background.getHeight()-24,AllIcons.I_CONFIRM);confirm.withCallback(this::onClose);addRenderableWidget(confirm);
        var flip=new IconButton(guiLeft+background.getWidth()-62,guiTop+background.getHeight()-24,AllIcons.I_FLIP);
        flip.setToolTip(Component.translatable("create.gui.threshold_switch.invert_signal"));flip.withCallback(()->{inverted=!inverted;dirty=true;send();});addRenderableWidget(flip);
        initializing=false;dirty=false;
    }
    @Override protected void renderWindow(GuiGraphics g,int x,int y,float partial){
        background.render(g,guiLeft,guiTop);g.drawCenteredString(font,title,guiLeft+background.getWidth()/2,guiTop+4,0x594424);
        AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS.render(g,guiLeft+44,guiTop+21);AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS.render(g,guiLeft+44,guiTop+45);
        g.drawString(font,"≥ "+tile.format(above.getState(),false).getString(),guiLeft+53,guiTop+28,0xffffff,true);
        g.drawString(font,"≤ "+tile.format(below.getState(),false).getString(),guiLeft+53,guiTop+52,0xffffff,true);
        g.renderItem(new ItemStack(net.minecraft.world.item.Items.REDSTONE_TORCH),guiLeft+22,guiTop+24);
        g.renderItem(new ItemStack(net.minecraft.world.item.Items.REDSTONE),guiLeft+22,guiTop+48);
        AllGuiTextures.THRESHOLD_SWITCH_CURRENT_STATE.render(g,guiLeft+20,guiTop+20+(inverted^tile.isPowered()?0:24));
        var amount=tile.currentLevel<0?"—":tile.currentLevel+"/"+tile.currentMaxLevel;
        g.drawString(font,amount,guiLeft+14,guiTop+84,0xffffff,true);
        GuiGameElement.of(new ItemStack(ArcaneCreateContent.SWITCH.get())).scale(5).at(guiLeft+background.getWidth()+6,guiTop+background.getHeight()-56,-200).render(g);
    }
    private void send(){if(!dirty||above==null||below==null)return;AllPackets.getChannel().sendToServer(new ConfigureThresholdSwitchPacket(tile.getBlockPos(),below.getState(),above.getState(),inverted,false));dirty=false;}
    @Override public void removed(){send();}
}
