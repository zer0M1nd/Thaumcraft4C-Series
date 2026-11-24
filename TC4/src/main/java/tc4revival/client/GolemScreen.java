package tc4revival.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.*;

public  class GolemScreen extends AbstractContainerScreen<GolemMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/guigolem.png");
    private final int threat;
    public GolemScreen(GolemMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        var random=menu.golem().getRandom(); threat=menu.golem().configuration().advanced() && random.nextInt(4)==0?random.nextInt(9):-1;
    }
    private record Toggle(int x,int y,int index,String label,boolean checked,boolean small,int color,boolean visible) {}
    private static String label(String key) { return Component.translatable("thaumcraft.golem."+key).getString(); }
    private List<Toggle> toggles() {
        var result=new ArrayList<Toggle>(); var config=menu.golem().configuration(); var core=config.core();
        result.add(new Toggle(8,72,18,Component.translatable("thaumcraft.golem.axis_aligned").getString(),config.axisAligned(),true,0xFDFDFD,true));
        if (core==GolemCoreType.GUARD) {
            String[] names={label("monsters"),label("animals"),label("players"),label("creepers")}; int[] colors={0xFFCCCC,0xFFFFCC,0xCCCCFF,0xCCFFCC};
            for (int i=0;i<4;i++) result.add(new Toggle(104,5+i*16,i+1,names[i],!config.toggle(i+1),false,colors[i],config.upgrades(4)>0));
        }
        if (core==GolemCoreType.FILL) result.add(new Toggle(62,54,0,config.toggle(0)?label("any_amount"):label("precise_amount"),!config.toggle(0),true,0xFDFDFD,true));
        if (core==GolemCoreType.USE) {
            String[] names={config.toggle(0)?label("empty_space"):label("block"),config.toggle(1)?label("left_click"):label("right_click"),config.toggle(2)?label("sneaking"):label("not_sneaking")};
            for (int i=0;i<3;i++) result.add(new Toggle(42,40+i*10,i,names[i],!config.toggle(i),true,0xFDFDFD,true));
        }
        if (core!=null && core.canSort && config.upgrades(5)>0) {
            int x=core==GolemCoreType.SORTING?66:180,y=core==GolemCoreType.SORTING?36:24;
            String[] names={label("use_material_tags"),label("ignore_item_damage"),label("ignore_nbt_values")};
            for (int i=0;i<3;i++) result.add(new Toggle(x,y+i*10,i+5,names[i],config.toggle(i+5),true,config.toggle(i+5)?0xFDFDFD:0x666666,true));
        }
        return result;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var config=menu.golem().configuration(); var core=config.core();
        if (core!=null && core.hasInventory) {
            for (int i=0;i<Math.min(6,menu.sampleCount());i++) {
                int x=leftPos+96+i/2*28,y=topPos+12+i%2*31;
                LegacyGui.blit(g,TEXTURE,x,y,184,config.material().ordinal()*24,24,24);
                if (core==GolemCoreType.LIQUID) {
                    var fluid=net.minecraftforge.fluids.FluidUtil.getFluidContained(menu.displayedSample(i)).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
                    if (!fluid.isEmpty()) {
                        var texture=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid);
                        if (texture!=null) {
                            var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
                            LegacyGui.blit(g,x+4,y+4,0,16,16,sprite);
                        }
                    }
                }
                if (config.upgrades(4)>0) {
                    LegacyGui.blit(g,TEXTURE,x,y-8,72,168,24,12); int color=menu.golem().filters().color(i+menu.page()*6);
                    if (color>=0) { int rgb=TallowCandleBlock.COLORS[color]; g.setColor((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,1); LegacyGui.blit(g,TEXTURE,x+9,y-5,0,176,6,6); g.setColor(1,1,1,1); }
                }
            }
            if (menu.sampleCount()>6) {
                LegacyGui.blit(g,TEXTURE,leftPos+111,topPos+68,0,menu.page()>0?200:208,24,8);
                LegacyGui.blit(g,TEXTURE,leftPos+135,topPos+68,24,menu.page()<menu.maxPage()?200:208,24,8);
            }
        }
        for (var toggle:toggles()) if (toggle.visible) {
            LegacyGui.blit(g,TEXTURE,leftPos+toggle.x,topPos+toggle.y,8,168,8,8);
            if (toggle.checked) LegacyGui.blit(g,TEXTURE,leftPos+toggle.x,topPos+toggle.y,8,176,8,8);
            if (core==GolemCoreType.FILL && toggle.index==0) small(g,toggle.label,leftPos+66-font.width(toggle.label)/4f,topPos+48,toggle.color);
            else if (toggle.small) small(g,toggle.label,leftPos+toggle.x+10,topPos+toggle.y+2,toggle.color);
            else g.drawString(font,toggle.label,leftPos+toggle.x+18,topPos+toggle.y+1,toggle.color,false);
        }
        GolemScreenPreview.render(g,menu.golem());
    }
    private void small(GuiGraphics g,String text,float x,float y,int color) { g.pose().pushPose(); g.pose().translate(x,y,0); g.pose().scale(.5f,.5f,.5f); g.drawString(font,text,0,0,color,false); g.pose().popPose(); }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {
        var core=menu.golem().configuration().core();
        if (core!=null) {
            var text=Component.translatable(threat>=0?"golemthreat."+threat+".text":"golemblurb."+core.ordinal()+".text");
            g.pose().pushPose(); g.pose().scale(.5f,.5f,.5f); int lineY=22;
            for (var line:font.split(text,110)) { g.drawString(font,line,80,lineY,0xDDDDDD,false); lineY+=font.lineHeight; }
            if (menu.maxPage()>0) g.drawString(font,(menu.page()+1)+"/"+(menu.maxPage()+1),323,140,0xDDDDDD,false);
            g.pose().popPose();
        }
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        renderBackground(g); menu.renderingFluidSlots(true);
        try { super.render(g,x,y,partial); } finally { menu.renderingFluidSlots(false); }
        renderTooltip(g,x,y);
        var config=menu.golem().configuration();
        // Original hover labels require entropy even though the color buttons require order.
        if (config.core()!=null && config.core().hasInventory && config.upgrades(5)>0) {
            for (int slot=0;slot<Math.min(6,menu.sampleCount());slot++) if (inside(x,y,96+slot/2*28,4+slot%2*31,24,12)) {
                int color=menu.golem().filters().color(slot+menu.page()*6); String name=color<0?label("any_color"):Component.translatable("color.minecraft."+net.minecraft.world.item.DyeColor.byId(color).getName()).getString();
                g.drawString(font,name,leftPos+133-font.width(name)/2,topPos-6,0xFDFDFD,false);
            }
        }
    }
    @Override protected void renderTooltip(GuiGraphics g,int x,int y) {
        if (hoveredSlot!=null && menu.isFluidSampleSlot(hoveredSlot)) {
            var fluid=net.minecraftforge.fluids.FluidUtil.getFluidContained(hoveredSlot.getItem()).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
            if (!fluid.isEmpty() && menu.getCarried().isEmpty()) g.renderTooltip(font,fluid.getDisplayName(),x,y);
        } else super.renderTooltip(g,x,y);
    }
    private boolean inside(double x,double y,int bx,int by,int width,int height) { return x>=leftPos+bx && x<leftPos+bx+width && y>=topPos+by && y<topPos+by+height; }
    private boolean button(int id) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id); return true; }
    @Override protected void containerTick() { super.containerTick(); if (!menu.golem().isAlive()) onClose(); }
    @Override public boolean mouseClicked(double x,double y,int button) {
        var config=menu.golem().configuration(); var core=config.core();
        if (core!=null && core.hasInventory) {
            if (config.upgrades(4)>0) for (int i=0;i<Math.min(6,menu.sampleCount());i++) {
                if (inside(x,y,96+i/2*28,4+i%2*31,8,12)) return button(i+menu.page()*6);
                if (inside(x,y,112+i/2*28,4+i%2*31,8,12)) return button(i+menu.page()*6+menu.sampleCount());
            }
            if (menu.page()>0 && inside(x,y,111,68,24,8)) return button(66);
            if (menu.page()<menu.maxPage() && inside(x,y,135,68,24,8)) return button(67);
        }
        for (var toggle:toggles()) if (inside(x,y,toggle.x,toggle.y,toggle.index>=5?64:8,8)) return button(50+toggle.index);
        return super.mouseClicked(x,y,button);
    }
}
