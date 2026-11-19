package automagy.client;
import automagy.content.InventariumMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class InventariumScreen extends AbstractContainerScreen<InventariumMenu>{
    private static final net.minecraft.resources.ResourceLocation TEXTURE=LegacyControls.texture("scribe_items");
    private EditBox search;private final int offset;
    public InventariumScreen(InventariumMenu menu,Inventory inv,Component title){super(menu,inv,title);offset=menu.requestTerminal()?30:0;imageWidth=207+offset;imageHeight=223;}
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();String previous=search==null?"":search.getValue();
        search=new LegacyEditBox(font,leftPos+28+offset,topPos+118,94,16,Component.translatable("Automagy.gui.scribe.search"));search.setMaxLength(80);search.setValue(previous);search.setResponder(s->automagy.content.FilterNetwork.search(menu.containerId,s));addRenderableWidget(search);setInitialFocus(search);
        if(menu.requestTerminal())addRenderableWidget(LegacyControls.recolored(leftPos+140,topPos+118,26,10,.5f,Component.translatable("Automagy.gui.scribe.mirrors"),()->send(6)));
        if(menu.hasLocalView())addRenderableWidget(LegacyControls.checkbox(leftPos+108+offset,topPos+119,LegacyControls.texture("eager_chest"),()->!menu.localView(),r->send(3)));
        addRenderableWidget(LegacyControls.checkbox(leftPos+147+offset,topPos+119,LegacyControls.texture("eager_chest"),menu::quantitySort,r->send(2)));
    }
    @Override protected void containerTick(){super.containerTick();search.tick();}
    @Override protected void slotClicked(net.minecraft.world.inventory.Slot slot,int id,int button,net.minecraft.world.inventory.ClickType type){
        if(id>=0&&id<InventariumMenu.DISPLAY_SIZE){
            if(menu.requestTerminal()&&slot!=null&&slot.hasItem()&&(type==net.minecraft.world.inventory.ClickType.PICKUP||type==net.minecraft.world.inventory.ClickType.QUICK_MOVE)){
                int delta=automagy.logic.RequisitionControls.delta(button,hasShiftDown(),hasControlDown(),slot.getItem().getMaxStackSize());
                if(delta!=0)automagy.content.FilterNetwork.request(menu.containerId,id,slot.getItem(),Math.abs(delta),delta<0,hasAltDown());
            }
            return;
        }
        super.slotClicked(slot,id,button,type);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){if(search.isFocused()&&key!=256){search.keyPressed(key,scan,mods);return true;}return super.keyPressed(key,scan,mods);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+offset,topPos,0,0,imageWidth-(menu.requestTerminal()?25:0),151);
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+offset,topPos+125,0,126,imageWidth-(menu.requestTerminal()?45:15),96);
        if(menu.requestTerminal()){
            tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos-24,topPos+23,194,151,61,105);
            tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+14,topPos+162,214,51,32,31);
            tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+22,topPos+169,240,menu.hasMirror()?17:0,16,16);
        }
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+12+offset,topPos+117,1,menu.page()>0?226:232,11,5);
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+184+offset,topPos+117,12,menu.page()+1<menu.pages()?226:232,12,5);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        if(menu.hasLocalView())LegacyControls.text(g,Component.translatable(menu.localView()?"Automagy.gui.scribe.itemsLocal":"Automagy.gui.scribe.itemsSystem"),117+offset,121,.5f,0x3f3f3f);
        LegacyControls.text(g,Component.translatable(menu.quantitySort()?"Automagy.gui.scribe.sortCount":"Automagy.gui.scribe.sortName"),156+offset,121,.5f,0x3f3f3f);
        if(menu.requestTerminal())LegacyControls.centered(g,Component.translatable("Automagy.gui.scribe.deliver"),7,27,.5f,0x3f3f3f);
        for(int i=0;i<InventariumMenu.DISPLAY_SIZE;i++){var slot=menu.slots.get(i);if(!slot.isActive()||!slot.hasItem())continue;long count=menu.amount(i);String text=count<10000?Long.toString(count):count<1000000?count/1000+"k":count/1000000+"M";
            g.pose().pushPose();g.pose().translate(0,0,210);g.pose().scale(.5f,.5f,1);g.drawString(font,text,(slot.x+17)*2-font.width(text),(slot.y+12)*2,0xffffff,true);g.pose().popPose();}
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&y>=topPos+114&&y<topPos+129){if(x>=leftPos+10+offset&&x<leftPos+25+offset){send(0);return true;}if(x>=leftPos+182+offset&&x<leftPos+198+offset){send(1);return true;}}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(x<leftPos+40&&menu.requestTerminal())send(delta>0?4:5);else send(delta>0?0:1);return true;}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);
        if(search.getValue().isEmpty()&&!search.isFocused())LegacyControls.text(g,Component.translatable("Automagy.gui.scribe.search").withStyle(net.minecraft.ChatFormatting.GRAY,net.minecraft.ChatFormatting.ITALIC),search.getX()+3,search.getY()+3,.75f,0xe0e0e0,true);
        if(hoveredSlot!=null&&hoveredSlot.hasItem()&&hoveredSlot.index<InventariumMenu.DISPLAY_SIZE){var lines=new java.util.ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));boolean demand=hoveredSlot.index>=InventariumMenu.PAGE_SIZE;
            lines.add(Component.translatable(demand?"gui.automagy.requisition.pending":"gui.automagy.inventarium.count",menu.amount(hoveredSlot.index)));
            if(menu.requestTerminal())lines.add(Component.translatable(demand?"gui.automagy.requisition.cancel":"gui.automagy.requisition.request"));g.renderTooltip(font,lines,java.util.Optional.empty(),x,y);
        }else renderTooltip(g,x,y);
    }
}
