package gadomancymodern.client;
import gadomancymodern.golem.GadGolems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class AdditionalGolemScreen extends tc4revival.client.GolemScreen {
    protected final int threat;
    public AdditionalGolemScreen(GolemMenu menu,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.network.chat.Component title){super(menu,inventory,title);var random=menu.golem().getRandom();threat=menu.golem().configuration().advanced()&&random.nextInt(4)==0?random.nextInt(9):-1;}
    @SubscribeEvent public static void opening(net.minecraftforge.client.event.ScreenEvent.Opening event){if(event.getNewScreen() instanceof tc4revival.client.GolemScreen screen&&!(screen instanceof AdditionalGolemScreen)){var golem=screen.getMenu().golem();var player=net.minecraft.client.Minecraft.getInstance().player;if(player!=null&&(GadGolems.silverwood(golem)||GadGolems.breaking(golem)))event.setNewScreen(new AdditionalGolemScreen(screen.getMenu(),player.getInventory(),screen.getTitle()));}}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mx,int my){super.renderBg(graphics,partial,mx,my);var golem=menu.golem();var core=golem.configuration().core();if(!GadGolems.silverwood(golem)||core==null||!core.hasInventory)return;
        for(int i=0;i<Math.min(6,menu.sampleCount());i++){int x=leftPos+96+i/2*28,y=topPos+12+i%2*31;tc4revival.client.LegacyGui.blit(graphics,net.minecraft.resources.ResourceLocation.parse("gadomancy:textures/gui/silverwood_slot.png"),x,y,0,0,24,24,24,24);if(core==GolemCoreType.LIQUID){var fluid=net.minecraftforge.fluids.FluidUtil.getFluidContained(menu.displayedSample(i)).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);if(!fluid.isEmpty()){var texture=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid);if(texture!=null)tc4revival.client.LegacyGui.blit(graphics,x+4,y+4,0,16,16,minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture));}}}
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mx,int my){if(!GadGolems.breaking(menu.golem())){super.renderLabels(graphics,mx,my);return;}var text=net.minecraft.network.chat.Component.translatable(threat>=0?"golemthreat."+threat+".text":"gadomancy.golemblurb.breakcore");graphics.pose().pushPose();graphics.pose().scale(.5f,.5f,.5f);int y=22;for(var line:font.split(text,110)){graphics.drawString(font,line,80,y,0xDDDDDD,false);y+=font.lineHeight;}if(menu.maxPage()>0)graphics.drawString(font,(menu.page()+1)+"/"+(menu.maxPage()+1),323,140,0xDDDDDD,false);graphics.pose().popPose();}
}
