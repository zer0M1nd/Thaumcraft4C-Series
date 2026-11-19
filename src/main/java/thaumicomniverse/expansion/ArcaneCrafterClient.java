package thaumicomniverse.expansion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumicomniverse",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArcaneCrafterClient {
    @SubscribeEvent public static void screens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){
        if(net.minecraftforge.fml.ModList.get().isLoaded("quark"))
            event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(ArcaneCrafter.MENU.get(),Screen::new));
    }
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        if(net.minecraftforge.fml.ModList.get().isLoaded("quark"))event.registerBlockEntityRenderer(ArcaneCrafter.TILE.get(),Renderer::new);
    }
    public static class Renderer implements BlockEntityRenderer<ArcaneCrafter.Tile> {
        public Renderer(BlockEntityRendererProvider.Context context){}
        @Override public void render(ArcaneCrafter.Tile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var wand=tile.getItem(9);if(wand.isEmpty())return;
            pose.pushPose();pose.translate(.65,1.0625,.25);pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(20));
            pose.mulPose(Axis.YP.rotationDegrees(-90));pose.scale(.5f,.5f,.5f);pose.translate(0,1,0);pose.mulPose(Axis.XP.rotationDegrees(180));
            tc4revival.client.WandRenderer.instance().renderModel(wand,pose,buffers,light,overlay,Minecraft.getInstance().player==null?0:Minecraft.getInstance().player.tickCount);
            pose.popPose();
        }
    }
    public static class Screen extends AbstractContainerScreen<ArcaneCrafter.Menu> {
        private static final ResourceLocation TEXTURE=ResourceLocation.parse("quark:textures/gui/container/crafter.png");
        public Screen(ArcaneCrafter.Menu menu,Inventory inventory,Component title){super(menu,inventory,title);}
        @Override protected void init(){super.init();titleLabelX=(imageWidth-font.width(title))/2;}
        @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
            graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
            graphics.blit(TEXTURE,leftPos+107,topPos+57,25,16,18,18);
            if(menu.tile.getBlockState().getValue(ArcaneCrafter.CRAFTING))graphics.blit(TEXTURE,leftPos+97,topPos+38,176,18,16,11);
            for(int i=0;i<9;i++)if(menu.tile.blocked(i)){
                var slot=menu.slots.get(i+1);graphics.blit(TEXTURE,leftPos+slot.x-1,topPos+slot.y-1,176,0,18,18);
            }
        }
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);
            if(hoveredSlot!=null&&!hoveredSlot.hasItem()&&menu.getCarried().isEmpty()){
                int slot=hoveredSlot.index;
                if(slot>=1&&slot<=9)graphics.renderTooltip(font,Component.translatable(menu.tile.blocked(slot-1)?"thaumicomniverse.crafter.enable":"quark.misc.disable_slot"),mouseX,mouseY);
                if(slot==10)graphics.renderTooltip(font,Component.translatable("thaumicomniverse.crafter.wand"),mouseX,mouseY);
            }
        }
        @Override public boolean mouseClicked(double x,double y,int button){
            // Only empty input cells can be toggled; neither the preview nor the wand slot is a grid cell.
            if(button==0&&menu.getCarried().isEmpty()){
                for(int i=1;i<=9;i++){
                    var slot=menu.slots.get(i);
                    if(!slot.hasItem()&&isHovering(slot.x,slot.y,16,16,x,y)){
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i-1);return true;
                    }
                }
            }
            return super.mouseClicked(x,y,button);
        }
    }
    private ArcaneCrafterClient(){}
}
