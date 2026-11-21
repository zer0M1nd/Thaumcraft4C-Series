package technomancy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import technomancy.*;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    public MachineScreen(MachineMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=menu.machine.kind.processor()?167:197;inventoryLabelY=imageHeight-94;}
    @Override protected void init(){
        super.init();if(menu.machine.kind.processor())return;
        if(menu.machine.kind==MachineKind.NODE_FABRICATOR||menu.machine.kind.catalyst())addRenderableWidget(Button.builder(Component.translatable("screen.technomancy.activate"),b->send(2)).bounds(leftPos+84,topPos+88,84,18).build());
        else if(menu.machine.kind==MachineKind.ECOLOGIC_TRANSMUTER||menu.machine.kind==MachineKind.MANA_EXCHANGER||menu.machine.kind==MachineKind.ELDRITCH_CONSUMER)addRenderableWidget(Button.builder(Component.translatable("screen.technomancy.mode"),b->send(1)).bounds(leftPos+84,topPos+88,84,18).build());
    }
    private void send(int action){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        var m=menu.machine;
        if(m.kind.processor()) {
            String name=m.kind==MachineKind.IGNIS_INCINERATOR?"processortc":m.kind==MachineKind.BOTANICAL_PURIFIER?"processorbo":"processorbm";
            g.blit(Technomancy.id("textures/gui/"+name+".png"),leftPos,topPos,0,0,175,167,256,256);
            if(m.progress>0)g.blit(Technomancy.id("textures/gui/"+name+".png"),leftPos+74,topPos+25,175,0,Math.min(20,m.progress/3),20,256,256);
        }else {
            g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff25202d);
            g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xffcdc6d3);
            for(var slot:menu.slots){int x=leftPos+slot.x-1,y=topPos+slot.y-1;g.fill(x,y,x+18,y+18,0xff625e68);g.fill(x+1,y+1,x+17,y+17,0xff97919c);}
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        var m=menu.machine;
        int labelX=m.kind.processor()?30:8;
        g.drawString(font,font.plainSubstrByWidth(title.getString(),m.kind.processor()?120:160),labelX,6,0x302437,false);
        int row=m.kind.processor()?49:20;
        if(m.kind.energyCapacity>0){g.drawString(font,Component.translatable("screen.technomancy.energy",m.energy,m.kind.energyCapacity),8,row,0x302437,false);row+=11;}
        if(m.kind.essentiaCapacity>0){g.drawString(font,Component.translatable("screen.technomancy.essentia",m.amount(),m.capacity()),labelX,row,0x302437,false);row+=11;}
        if(!m.kind.dependency.isEmpty()&&m.kind.dependency.equals("botania")){g.drawString(font,Component.translatable("screen.technomancy.mana",m.mana),labelX,row,0x302437,false);row+=11;}
        if(ExistenceLogic.capacity(m.kind)>0){g.drawString(font,Component.translatable("screen.technomancy.existence",ExistenceLogic.power(m),ExistenceLogic.capacity(m.kind)),8,row,0x302437,false);row+=11;}
        if(m.fluid.getFluidAmount()>0){g.drawString(font,Component.translatable("screen.technomancy.fluid",m.fluid.getFluidAmount()),8,row,0x302437,false);row+=11;}
        if(!m.kind.processor())g.drawString(font,Component.translatable("screen.technomancy.redstone_"+m.redstone),8,Math.min(row,56),0x57445e,false);
        if(m.kind==MachineKind.ECOLOGIC_TRANSMUTER||m.kind==MachineKind.MANA_EXCHANGER||m.kind==MachineKind.ELDRITCH_CONSUMER)g.drawString(font,Component.translatable("mode.technomancy."+m.kind.id()+"."+m.mode),8,67,0x57445e,false);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);}
}
