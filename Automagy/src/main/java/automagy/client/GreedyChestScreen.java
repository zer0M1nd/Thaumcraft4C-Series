package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
public class GreedyChestScreen extends AbstractContainerScreen<GreedyChestMenu>{
    private static final ResourceLocation CONFIG=LegacyControls.texture("greedy_chest"),CHEST=ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");
    private EditBox quantity;private int lastMode=-1,lastValue=-1;
    public GreedyChestScreen(GreedyChestMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=menu.configuration?166:168;inventoryLabelY=imageHeight-94;}
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();if(!menu.configuration)return;
        for(int i=0;i<6;i++){int side=i;int x=i==0||i==3||i==5?50:5,y=i<2?65:i<4?45:55;
            addRenderableWidget(LegacyControls.checkbox(leftPos+x,topPos+y,CONFIG,()->menu.value(side)!=0,r->send(side)));}
        addRenderableWidget(LegacyControls.small(leftPos,topPos+10,95,15,()->Component.translatable("gui.automagy.greedy.mode."+menu.value(6)),r->send(r?14:6)));
        quantity=new LegacyEditBox(font,leftPos+102,topPos+11,38,16,Component.translatable("Automagy.gui.greedyChest.quantityResult.open"));quantity.setMaxLength(5);quantity.setTextColor(0xffffff);
        quantity.setFilter(s->s.isEmpty()||s.matches(menu.value(6)<2?"[0-9]{0,3}([.][0-9]{0,2})?":"[0-9]{0,5}"));
        quantity.setResponder(s->{if(!quantity.isFocused()||s.isBlank())return;try{FilterNetwork.greedyQuantity(menu.containerId,menu.value(6),Double.parseDouble(s));}catch(NumberFormatException ignored){}});addRenderableWidget(quantity);}
    @Override protected void containerTick(){super.containerTick();if(quantity==null)return;quantity.tick();int mode=menu.value(6),value=menu.value(mode<2?7:8);
        if(mode!=lastMode||value!=lastValue&&!quantity.isFocused()){quantity.setValue(mode<2?String.format(java.util.Locale.ROOT,"%.2f",value/100d):Integer.toString(value));lastMode=mode;lastValue=value;}}
    @Override public boolean keyPressed(int key,int scan,int mods){if(quantity!=null&&quantity.isFocused()&&key!=256){quantity.keyPressed(key,scan,mods);return true;}return super.keyPressed(key,scan,mods);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){if(menu.configuration)tc4revival.client.LegacyGui.blit(g,CONFIG,leftPos,topPos,0,0,176,166);else{tc4revival.client.LegacyGui.blit(g,CHEST,leftPos,topPos,0,0,176,71);tc4revival.client.LegacyGui.blit(g,CHEST,leftPos,topPos+71,0,126,176,96);}}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){if(!menu.configuration){super.renderLabels(g,x,y);return;}
        LegacyControls.text(g,Component.translatable("Automagy.gui.greedyChest.mode.name"),2,1,.75f,0xffffff);
        LegacyControls.text(g,Component.translatable("Automagy.gui.greedyChest.dir.name"),2,32,.75f,0xffffff);
        for(int i=0;i<6;i++){int sx=i==0||i==3||i==5?62:17,sy=i<2?66:i<4?46:56;LegacyControls.text(g,Component.translatable("Automagy.gui.greedyChest.dir."+new String[]{"D","U","N","S","W","E"}[i]),sx,sy,.75f,0xffffff);}
        if(menu.value(6)<2)LegacyControls.text(g,Component.literal("%"),133,14,.75f,0xffffff);
        var filter=menu.getSlot(27).getItem();
        if(filter.getItem() instanceof FilterItem item&&ItemFilter.load(filter).useCount){
            String meaning=switch(menu.value(6)){case 2->item.blacklist?"override":"limit";case 3->item.blacklist?"leave":"override";default->item.blacklist?"leave":"limit";};
            LegacyControls.text(g,Component.translatable("Automagy.gui.greedyChest.quantityResult.open"),120,54,.525f,0xffffff);
            LegacyControls.text(g,Component.translatable("Automagy.gui.greedyChest.quantityResult."+meaning),120,60,.525f,0xffffff);
        }}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
