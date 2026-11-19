package temodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import temodern.crafting.WorkMenu;

/** Modern artwork preserves the existing menu slot and preview coordinates. */
public final class WorkScreen extends AbstractContainerScreen<WorkMenu> {
    public WorkScreen(WorkMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=247;imageHeight=197;inventoryLabelY=103;}
    @Override protected void init(){super.init();
        if(menu.kind.equals("distillation_encoder"))addRenderableWidget(Button.builder(Component.translatable("gui.thaumicenergistics.encode_distillation"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,2)).bounds(leftPos+104,topPos+28,64,20).build());
    }

    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        tc4revival.client.LegacyGui.blit(g,new net.minecraft.resources.ResourceLocation("thaumicenergistics","textures/modern/gui/arcane_assembler.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        var patterns=temodern.item.KnowledgeCore.patterns(menu.inventory.getItem(10));
        for(int i=0;i<Math.min(21,patterns.size());i++){
            var pattern=new temodern.crafting.ArcanePattern.Decoder().decodePattern(patterns.get(i),minecraft.level,false);
            if(pattern!=null)g.renderItem(((appeng.api.stacks.AEItemKey)pattern.getPrimaryOutput().what()).toStack(),leftPos+26+i%7*18,topPos+25+i/7*18);
        }
        int index=0;
        for(var aspect:thaumcraft.api.aspects.AspectSelection.select(tc4revival.aspect.TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.LEGACY_SIX)){
            int bar=switch(aspect.tag()){case "aer"->41;case "aqua"->59;case "ignis"->77;case "ordo"->95;case "perditio"->113;default->131;};
            int x=leftPos+bar,y=topPos+87,amount=menu.storedVis(index++),height=Math.min(16,amount*16/15000);
            if(height>0)g.fill(x,y+16-height,x+4,y+16,0xff000000|aspect.color());
            if(mx>=x&&mx<x+4&&my>=y&&my<y+16)g.renderTooltip(font,Component.literal(aspect.tag()+": "+(amount/100.0)+" / 150 vis"),mx,my);
        }

    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
}
