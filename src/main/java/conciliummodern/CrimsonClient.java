package conciliummodern;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CrimsonClient {
    public static final KeyMapping ROBE=new KeyMapping("key.thaumicconcilium.pontifex",net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,net.minecraftforge.client.settings.KeyModifier.ALT,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_L),"key.categories.misc");
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->
        thaumcraft.api.client.ResearchPresentationClient.registerTextToken("thaumicconcilium:robe_key",0,"entry.pontifexrobe.first",(locale,text)->{
            String binding=ROBE.getTranslatedKeyMessage().getString();
            return text.replace("Press L to toggle this ability.","Press "+binding+" to toggle this ability.")
                .replace("按下H键切换此功能","按下"+binding+"切换此功能").replace("按下H鍵切換此功能","按下"+binding+"切換此功能");
        }));}
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(ROBE);}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){event.register((stack,index)->index==0?((PontifexRobe)stack.getItem()).getColor(stack):0xffffff,CrimsonContent.HELMET.get(),CrimsonContent.CHEST.get(),CrimsonContent.LEGS.get(),CrimsonContent.BOOTS.get());}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)resources->CrimsonObjRenderer.reload());}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws java.io.IOException{CrimsonOrbRenderer.shaders(event);CrimsonObjRenderer.shaders(event);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(CrimsonContent.PALADIN.get(),Brigade::new);event.registerEntityRenderer(CrimsonContent.ARCHER.get(),Brigade::new);event.registerEntityRenderer(CrimsonContent.RANGER.get(),Brigade::new);
        event.registerEntityRenderer(CrimsonContent.PONTIFEX.get(),Pontifex::new);event.registerEntityRenderer(CrimsonContent.PORTAL.get(),LesserPortalRenderer::new);
        event.registerEntityRenderer(CrimsonContent.SHACKLES.get(),CrimsonObjRenderer.Shackles::new);event.registerEntityRenderer(CrimsonContent.ORB.get(),CrimsonOrbRenderer::new);
    }
    private static final class Brigade extends HumanoidMobRenderer<CrimsonBrigade,HumanoidModel<CrimsonBrigade>>{
        Brigade(EntityRendererProvider.Context context){super(context,new HumanoidModel<>(net.minecraft.client.model.geom.builders.LayerDefinition.create(HumanoidModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,0),64,32).bakeRoot()),.5f);model.hat.visible=false;
            addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),context.getModelManager()));}
        @Override public ResourceLocation getTextureLocation(CrimsonBrigade entity){return entity.getType()==CrimsonContent.RANGER.get()?ResourceLocation.parse("thaumcraft:textures/models/cultist.png"):ResourceLocation.parse("thaumicconcilium:textures/entity/"+(entity.getType()==CrimsonContent.PALADIN.get()?"crimson_paladin":"crimson_archer")+".png");}
        @Override public void render(CrimsonBrigade entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
            model.rightArmPose=entity.getType()==CrimsonContent.ARCHER.get()&&entity.isAggressive()?HumanoidModel.ArmPose.BOW_AND_ARROW:HumanoidModel.ArmPose.ITEM;super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private static final class Pontifex extends HumanoidMobRenderer<CrimsonPontifex,HumanoidModel<CrimsonPontifex>>{
        Pontifex(EntityRendererProvider.Context context){super(context,new HumanoidModel<>(net.minecraft.client.model.geom.builders.LayerDefinition.create(HumanoidModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,0),64,32).bakeRoot()),1f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),context.getModelManager()));}
        @Override public ResourceLocation getTextureLocation(CrimsonPontifex entity){return ResourceLocation.parse("thaumcraft:textures/models/cultist.png");}
    }
}
