package thaumicomniverse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.EntityRenderersEvent;
import mods.railcraft.client.gui.screen.inventory.ManipulatorScreen;
import thaumicomniverse.compat.railcraft.*;

/** Loaded only from guarded client entry points. */
public final class ArcaneRailClient {
    public static void setup(){MenuScreens.register(ArcaneRail.MENU.get(),Screen::new);}
    public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ArcaneRail.CART.get(),Renderer::new);}
    public static class Renderer extends MinecartRenderer<EssentiaMinecart> {
        public Renderer(EntityRendererProvider.Context context){super(context,ModelLayers.MINECART);}
        @Override protected void renderMinecartContents(EssentiaMinecart cart,float partial,BlockState state,PoseStack pose,MultiBufferSource buffers,int light){
            var jar=new ItemStack(tc4revival.content.TCItems.FILLED_ESSENTIA_JAR.get());jar.setTag(cart.tank.save());
            // MinecartRenderer scales its display block to 3/4. Keep a full-size jar centered in the cart.
            pose.pushPose(); pose.translate(.5, 0, .5); pose.scale(4f/3f, 4f/3f, 4f/3f); pose.translate(-.5, 0, -.5);
            tc4revival.client.EssentiaJarRenderer.itemRenderer().renderByItem(jar,ItemDisplayContext.NONE,pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
    public static class Screen extends ManipulatorScreen<EssentiaManipulatorMenu> {
        public Screen(EssentiaManipulatorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
        @Override protected void sendAttributes(){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,menu.getManipulator().getRedstoneMode().ordinal());}
        @Override public ResourceLocation getWidgetsTexture(){return ResourceLocation.parse("thaumicomniverse:textures/gui/essentia_manipulator.png");}
        @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
            super.renderBg(graphics,partial,mouseX,mouseY);var tank=menu.getManipulator().tank;var aspect=tank.aspect();
            if(aspect!=null){
                int height=48*tank.amount()/tank.limit();graphics.fill(leftPos+17,topPos+68-height,leftPos+33,topPos+68,0xff000000|aspect.color());
                graphics.blit(aspect.texture(),leftPos+17,topPos+21,0,0,16,16,16,16);
            }
        }
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            super.render(graphics,mouseX,mouseY,partial);
            if(isHovering(16,20,18,49,mouseX,mouseY)){
                var tank=menu.getManipulator().tank;
                graphics.renderTooltip(font,Component.translatable("thaumicomniverse.rail.contents",tank.aspect()==null?Component.translatable("omniverse.machine.empty"):Component.literal(thaumcraft.api.aspects.AspectViews.display(tank.aspect()).displayName()),tank.amount(),tank.limit()),mouseX,mouseY);
            }
        }
    }
    private ArcaneRailClient(){}
}
