package thaumicomniverse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumicomniverse.ThaumicOmniverse;
import thaumicomniverse.alignment.*;

public class AlignmentScreen extends AbstractContainerScreen<AlignmentMenu> {
    private int offset;
    private Button start;
    public AlignmentScreen(AlignmentMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);imageWidth=292;imageHeight=242;}
    public static void receive(AlignmentNetwork.View packet) {var player=Minecraft.getInstance().player;if(player!=null&&player.containerMenu instanceof AlignmentMenu menu&&menu.containerId==packet.menu()&&packet.tag()!=null)menu.view=packet.tag();}
    @Override protected void init() {
        super.init();start=addRenderableWidget(Button.builder(Component.translatable("thaumicomniverse.alignment.start"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+196,topPos+215,86,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;g.fill(x,y,x+imageWidth,y+imageHeight,0xff292536);g.fill(x+4,y+4,x+288,y+238,0xffded7c9);
        g.fill(x+10,y+47,x+282,y+172,0xff343042);
        var rows=menu.view.getList("rows",Tag.TAG_COMPOUND);offset=Math.max(0,Math.min(offset,rows.size()-8));
        for(int i=offset;i<Math.min(rows.size(),offset+8);i++) {
            var row=rows.getCompound(i);int ry=y+51+(i-offset)*15;String id=row.getString("aspect");
            String label=tc4revival.aspect.TCAspects.catalog().find(id).map(visibleAspect->thaumcraft.api.aspects.AspectViews.display(visibleAspect).displayName()).orElse(id);
            g.drawString(font,label,x+14,ry,0xffeee5d3,false);g.drawString(font,Long.toString(row.getLong("current")),x+122,ry,0xffeee5d3,false);
            g.drawString(font,Long.toString(row.getLong("target")),x+178,ry,0xffeee5d3,false);g.drawString(font,Long.toString(row.getLong("delta")),x+236,ry,row.getLong("delta")==0?0xff98dfa2:0xffffa29d,false);
        }
        int remaining=menu.view.getInt("remaining"),total=menu.view.getInt("total");
        start.active=remaining==0&&menu.view.getBoolean("exact")&&menu.view.getBoolean("ingredients");
        if(remaining>0&&total>0){g.fill(x+12,y+183,x+280,y+194,0xff53485d);g.fill(x+12,y+183,x+12+(total-remaining)*268/total,y+194,0xffa77bd1);}
        var result=ItemStack.of(menu.view.getCompound("result"));if(!result.isEmpty()){g.renderItem(result,x+12,y+214);g.renderItemDecorations(font,result,x+12,y+214);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,10,10,0xff332c43,false);
        g.drawString(font,Component.translatable("thaumicomniverse.alignment.columns"),12,31,0xff53445e,false);
        String recipe=menu.view.getString("recipe");int running=menu.view.getInt("remaining");
        if(running>0)g.drawString(font,Component.translatable("thaumicomniverse.alignment.running",running),12,199,0xff53445e,false);
        else if(recipe.isEmpty())g.drawWordWrap(font,Component.translatable("thaumicomniverse.alignment.no_recipe"),12,177,268,0xff704e6a);
        else {
            g.drawString(font,Component.translatable("thaumicomniverse.alignment.preview",menu.view.getInt("yield"),String.format(java.util.Locale.ROOT,"%.2f",menu.view.getDouble("risk"))),12,178,0xff53445e,false);
            g.drawString(font,Component.translatable("thaumicomniverse.alignment.materials",menu.view.getLong("count"),menu.view.getInt("kinds")),12,192,0xff53445e,false);
            g.drawString(font,Component.translatable(menu.view.getBoolean("exact")&&menu.view.getBoolean("ingredients")?"thaumicomniverse.alignment.ready":"thaumicomniverse.alignment.mismatch"),36,218,0xff53445e,false);
        }
        if(menu.view.getBoolean("unknown"))g.drawString(font,"?",270,32,0xff665078,false);
        if(!menu.view.getString("diagnostic").isEmpty())g.drawWordWrap(font,Component.literal(menu.view.getString("diagnostic")),12,178,268,0xffa42c38);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {renderBackground(g);super.render(g,mouseX,mouseY,partial);}
    @Override public boolean mouseScrolled(double x,double y,double delta) {offset=Math.max(0,offset-(int)Math.signum(delta));return true;}
    @Mod.EventBusSubscriber(modid=ThaumicOmniverse.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {event.enqueueWork(()->MenuScreens.register(AlignmentContent.MENU.get(),AlignmentScreen::new));}
        @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(AlignmentContent.PEDESTAL_ENTITY.get(),tc4revival.client.ArcanePedestalRenderer::new);
            event.registerBlockEntityRenderer(AlignmentContent.CORE_ENTITY.get(),tc4revival.client.ArcanePedestalRenderer::new);
        }
    }
}
