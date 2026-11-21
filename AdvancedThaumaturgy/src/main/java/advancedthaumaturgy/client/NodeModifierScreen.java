package advancedthaumaturgy.client;
import advancedthaumaturgy.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class NodeModifierScreen extends AbstractContainerScreen<NodeModifierMenu>{
    private final Map<NodeOperation,Button> buttons=new EnumMap<>(NodeOperation.class);
    public NodeModifierScreen(NodeModifierMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=256;imageHeight=222;}
    @Override protected void init(){super.init();buttons.clear();for(var op:NodeOperation.values()){
        var button=Button.builder(Component.translatable(op.key()),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,op.ordinal())).bounds(leftPos+8+(op.ordinal()%2)*122,topPos+27+(op.ordinal()/2)*23,118,20).build();
        button.setTooltip(Tooltip.create(Component.translatable("advancedthaumaturgy.node.cost",op.amount,aspectName(op.essentia),op.essences,op.essence.isEmpty()?Component.literal("*"):aspectName(op.essence))));buttons.put(op,addRenderableWidget(button));
    }}
    private static Component aspectName(String tag){return thaumcraft.api.client.ClientAspectViews.view(tc4revival.aspect.TCAspects.catalog().require(tag)).name();}
    @Override protected void containerTick(){super.containerTick();var available=menu.entity.available();buttons.forEach((op,b)->b.active=!menu.entity.active()&&available.contains(op));}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffd4c9ab);g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xffeee2c5);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,8,10,0x403028,false);g.drawString(font,Component.translatable(menu.entity.active()?"advancedthaumaturgy.node.progress":"advancedthaumaturgy.node.ready",menu.entity.remainingEssentia(),menu.entity.remainingEssences()),8,197,0x403028,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);}
}
