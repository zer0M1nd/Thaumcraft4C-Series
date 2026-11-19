package ttmodern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class AspectAnalyzerScreen extends AbstractContainerScreen<AspectAnalyzerMenu> {
    private int offset;
    public AspectAnalyzerScreen(AspectAnalyzerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Tinkerer.ASPECT_ANALYZER_MENU.get(),AspectAnalyzerScreen::new));}
    private java.util.List<tc4revival.research.InventoryAspectDisplay.Icon> icons(){return tc4revival.research.InventoryAspectDisplay.resolve(menu.analyzer.items.getStackInSlot(0),tc4revival.client.ClientKnowledge.snapshot().orElse(null));}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":textures/gui/aspect_analyzer.png"),leftPos,topPos,0,0,176,166);var icons=icons();offset=Math.min(offset,Math.max(0,icons.size()-8));for(int i=offset;i<Math.min(icons.size(),offset+8);i++){var icon=icons.get(i);int ix=leftPos+20+(i-offset)*18,iy=topPos+58;var aspect=tc4revival.aspect.TCAspects.catalog().find(icon.tag());if(aspect.isPresent()){int color=aspect.get().color();g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);tc4revival.client.LegacyGui.blit(g,aspect.get().texture(),ix,iy,0,0,16,16,16,16);g.setColor(1,1,1,1);g.pose().pushPose();g.pose().translate(ix+10,iy+10,200);g.pose().scale(.5f,.5f,1);g.drawString(font,Integer.toString(icon.amount()),0,0,0xffffff);g.pose().popPose();}else g.drawString(font,"?",ix+4,iy+4,0xaaaaaa);}}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(x>=leftPos+20&&x<leftPos+164&&y>=topPos+54&&y<topPos+78){offset=Math.max(0,Math.min(Math.max(0,icons().size()-8),offset+(amount<0?1:-1)));return true;}return super.mouseScrolled(x,y,amount);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);if(x>=leftPos+20&&x<leftPos+164&&y>=topPos+58&&y<topPos+74){var values=icons();int index=offset+(x-leftPos-20)/18;if(index<values.size())tc4revival.aspect.TCAspects.catalog().find(values.get(index).tag()).ifPresent(aspect->g.renderTooltip(font,java.util.List.of(Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()),Component.translatable(thaumcraft.api.aspects.AspectViews.display(aspect).descriptionKey())),java.util.Optional.empty(),x,y));}}
}
