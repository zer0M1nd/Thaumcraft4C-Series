package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
public class EagerChestScreen extends AbstractContainerScreen<EagerChestMenu>{
    private static final ResourceLocation CHEST=ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png"),CONFIG=LegacyControls.texture("eager_chest");
    public EagerChestScreen(EagerChestMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=menu.configuration?166:168;inventoryLabelY=imageHeight-94;}
    @Override protected void init(){super.init();if(menu.configuration)addRenderableWidget(LegacyControls.checkbox(leftPos+30,topPos+65,CONFIG,menu::enabled,r->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)));}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){if(menu.configuration)tc4revival.client.LegacyGui.blit(g,CONFIG,leftPos,topPos,0,0,176,166);else{tc4revival.client.LegacyGui.blit(g,CHEST,leftPos,topPos,0,0,176,71);tc4revival.client.LegacyGui.blit(g,CHEST,leftPos,topPos+71,0,126,176,96);}}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){if(!menu.configuration){super.renderLabels(g,x,y);return;}
        LegacyControls.centered(g,Component.translatable("Automagy.gui.eagerChest.give"),27,0,.75f,0xffffff);
        LegacyControls.centered(g,Component.translatable("Automagy.gui.eagerChest.take"),113,0,.75f,0xffffff);
        LegacyControls.text(g,Component.translatable("Automagy.gui.eagerChest.trade"),42,66,.75f,0xffffff);
        for(int i=0;i<4;i++){var stack=menu.slots.get(27+i).getItem();if(stack.isEmpty())continue;
            var f=ItemFilter.load(stack);if(!f.useCount)continue;String key="Automagy.gui.eagerChest.quantityResult."+(i<2?"give":"take")+(stack.getItem() instanceof FilterItem item&&item.blacklist?"Blacklist.":"Whitelist.");
            LegacyControls.text(g,Component.translatable(key+"1"),40+(i/2)*87,16+(i%2)*28,.525f,0xffffff);
            LegacyControls.text(g,Component.translatable(key+"2"),40+(i/2)*87,22+(i%2)*28,.525f,0xffffff);}}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
