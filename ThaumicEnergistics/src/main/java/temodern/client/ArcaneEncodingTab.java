package temodern.client;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicenergistics",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ArcaneEncodingTab {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void init(net.minecraftforge.client.event.ScreenEvent.Init.Post event){
        if(!(event.getScreen() instanceof appeng.client.gui.me.items.PatternEncodingTermScreen<?> screen))return;
        var menu=(temodern.crafting.ArcaneEncodingMenu)screen.getMenu();
        var title=net.minecraft.network.chat.Component.translatable("gui.thaumicenergistics.arcane_mode");
        var button=new appeng.client.gui.widgets.TabButton(new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.ARCANE_WORKBENCH.get()),title,b->menu.teSetArcane(true)) {
            @Override public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics,int x,int y,float partial){
                setSelected(menu.teIsArcane());
                super.renderWidget(graphics,x,y,partial);
            }
        };
        button.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        button.setPosition(screen.getGuiLeft()+173,screen.getGuiTop()+screen.getYSize()-90);
        event.addListener(button);
    }
}
