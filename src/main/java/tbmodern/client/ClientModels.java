package tbmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tbmodern.*;

@Mod.EventBusSubscriber(modid="thaumicbases", bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class ClientModels {
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for(String skin:event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer=event.getSkin(skin);
            if(renderer!=null)renderer.addLayer(new BraceletLayer(renderer));
        }
    }
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {event.registerReloadListener(RevolverRenderer.RELOAD);event.registerReloadListener(BraceletRenderer.RELOAD);event.registerReloadListener(NodeDeviceModels.RELOAD);}
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(Content.OVERCHANTER_MENU.get(), OverchanterScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.REVOLVER_MENU.get(), RevolverScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.THAUMIC_ANVIL_MENU.get(), MagicalAnvilScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.VOID_ANVIL_MENU.get(), MagicalAnvilScreen::new);
            for(String id:java.util.List.of("thaumic_anvil","void_anvil")) net.minecraft.client.renderer.item.ItemProperties.register(Content.item(id),new net.minecraft.resources.ResourceLocation("thaumicbases","anvil_damage"),(stack,level,entity,seed)->MagicalAnvil.itemDamage(stack));
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Content.REVOLVER_BULLET.get(),context->new net.minecraft.client.renderer.entity.EntityRenderer<RevolverBullet>(context){
            @Override public net.minecraft.resources.ResourceLocation getTextureLocation(RevolverBullet bullet){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
        });
        event.registerBlockEntityRenderer(Content.OVERCHANTER.get(), context -> new BlockEntityRenderer<Overchanter>() {
            @Override public void render(Overchanter tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                if(tile.getItem(0).isEmpty())return;
                pose.pushPose();pose.translate(.5,.77,.5);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(30));pose.scale(.7F,.7F,.7F);
                Minecraft.getInstance().getItemRenderer().renderStatic(tile.getItem(0),ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),0);
                pose.popPose();
            }
        });
        event.registerBlockEntityRenderer(Content.MANIPULATOR.get(), context -> new ManipulatorRenderer());
        event.registerBlockEntityRenderer(Content.LINKER.get(), context -> new LinkRenderer());
        event.registerBlockEntityRenderer(Content.DECONSTRUCTOR.get(), context -> new DeconstructorRenderer());
    }
    private static final class DeconstructorRenderer implements BlockEntityRenderer<EntityDeconstructor> {
        @Override public void render(EntityDeconstructor entity,float tick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
            for(int i=0;i<6;i++)if((entity.crystals()&(1<<i))!=0) {
                double angle=i*Math.PI/3;
                poses.pushPose();poses.translate(.5+Math.cos(angle)*.38,.17,.5+Math.sin(angle)*.38);poses.scale(.12F,.12F,.12F);
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Content.item(Content.CRYSTALS.get(i))),ItemDisplayContext.FIXED,15728880,overlay,poses,buffers,entity.getLevel(),i);
                poses.popPose();
            }
        }
    }
    private static final class LinkRenderer implements BlockEntityRenderer<NodeLinker> {
        @Override public void render(NodeLinker entity,float tick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
            NodeDeviceModels.render(false,"",poses,buffers,light,overlay);
        }
    }
    private static final class ManipulatorRenderer implements BlockEntityRenderer<NodeManipulator> {
        @Override public void render(NodeManipulator entity, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
            NodeDeviceModels.render(true,entity.mode(),poses,buffers,light,overlay);
        }
    }
    private ClientModels() {}
}
