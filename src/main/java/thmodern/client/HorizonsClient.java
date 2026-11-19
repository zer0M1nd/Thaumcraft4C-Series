package thmodern.client;

import net.minecraft.client.KeyMapping;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HorizonsClient {
    @SubscribeEvent public static void dimensions(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event){event.register(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:pocket_plane"),new PocketPlaneEffects());}
    @SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event){event.register(LensClient.SELECT = new KeyMapping("key.thaumichorizons.lenses",org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,"key.categories.thaumichorizons"));event.register(BodyKeys.FINGERS = new KeyMapping("key.thaumichorizons.fingers",org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,"key.categories.thaumichorizons"));event.register(BodyKeys.CLIMB = new KeyMapping("key.thaumichorizons.climb",org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,"key.categories.thaumichorizons"));event.register(BodyKeys.INVISIBLE = new KeyMapping("key.thaumichorizons.invisible",org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,"key.categories.thaumichorizons"));}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{thaumcraft.api.client.ResearchPageRenderers.register(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:vat_structure"),data->new VatStructurePage());MenuScreens.register(Horizons.LENS_CASE_MENU.get(),LensCaseScreen::new);MenuScreens.register(Horizons.SOUL_DEVICE_MENU.get(),SoulDeviceScreen::new);MenuScreens.register(Horizons.FINGERS_MENU.get(),MorphicFingersScreen::new);MenuScreens.register(Horizons.VAT_MENU.get(),VatScreen::new);MenuScreens.register(Horizons.VIS_DYNAMO_MENU.get(),VisDynamoScreen::new);MenuScreens.register(Horizons.BLOOD_INFUSER_MENU.get(),BloodInfuserScreen::new);MenuScreens.register(Horizons.INJECTOR_MENU.get(),InjectorScreen::new);});}
    @SubscribeEvent public static void golems(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event){AnimatedGolemRendering.renderer=new BlockGolemRenderer(event.getContext());}
    @SubscribeEvent public static void itemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->thmodern.plane.KeystoneItem.color(stack),Horizons.KEYSTONE.get());
        event.register((stack,tint)->net.minecraft.world.item.alchemy.PotionUtils.getColor(stack),Horizons.INJECTION.get(),Horizons.INJECTION_PHIAL.get());
        event.register((stack,tint)->0xff0000,Horizons.HUMAN_BLOOD.get(),Horizons.BLOOD_SAMPLE.get());
        event.register((stack,tint)->{
            if(!stack.hasTag()||!stack.getTag().contains("BlockAppearance"))return 0xff000000;
            var state=net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),stack.getTag().getCompound("BlockAppearance"));
            return 0xff000000|state.getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO).col;
        },Horizons.GOLEM_PLACER.get());
    }
    @SubscribeEvent public static void blockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event){event.register((state,level,pos,tint)->level!=null&&pos!=null&&level.getBlockEntity(pos) instanceof thmodern.plane.KeystoneSlotEntity slot?thmodern.plane.KeystoneItem.color(slot.key()):0xffffff,thmodern.plane.GatewayBlocks.SLOT.get());}
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(Horizons.TRANSDUCTION_AMPLIFIER_ENTITY.get(),AmplifierRenderer::new);
        event.registerBlockEntityRenderer(thmodern.plane.PlanarDecorations.SPIKE_ENTITY.get(),SpikeRenderer::new);
        event.registerBlockEntityRenderer(thmodern.plane.CloudBlocks.ENTITY.get(),CloudRenderer::new);
        event.registerEntityRenderer(thmodern.plane.CloudBlocks.LIGHTNING.get(),FiniteLightningRenderer::new);
        event.registerBlockEntityRenderer(thmodern.plane.GatewayBlocks.SLOT_ENTITY.get(),GatewaySlotRenderer::new);
        event.registerBlockEntityRenderer(Horizons.VORTEX_ENTITY.get(),VortexRenderer::new);
        event.registerBlockEntityRenderer(Horizons.SOUL_BEACON_ENTITY.get(),SoulBeaconRenderer::new);
        event.registerBlockEntityRenderer(Horizons.INSPIRATRON_ENTITY.get(),SoulDeviceRenderer::new);
        event.registerBlockEntityRenderer(Horizons.SOULFORGE_ENTITY.get(),SoulDeviceRenderer::new);
        event.registerBlockEntityRenderer(Horizons.SOUL_EXTRACTOR_ENTITY.get(),SoulDeviceRenderer::new);
        event.registerEntityRenderer(Horizons.GREATWOOD_BOAT.get(),HorizonsBoatRenderer::new);event.registerEntityRenderer(Horizons.THAUMIUM_BOAT.get(),HorizonsBoatRenderer::new);
        event.registerEntityRenderer(Horizons.ALCHEMITE_ENTITY.get(),AlchemiteRenderer::new);
        for(var type:thmodern.creature.PlanarSlimes.TYPES.values())event.registerEntityRenderer(type.get(),PlanarSlimeRenderer::new);
        event.registerEntityRenderer(Horizons.SOUL.get(),SoulRenderer::new);
        for(var type:java.util.List.of(Horizons.GUARDIAN_PANTHER,Horizons.GRAVEKEEPER,Horizons.FAMILIAR))event.registerEntityRenderer(type.get(),InfusedCatRenderer::new);
        event.registerEntityRenderer(Horizons.NIGHTMARE.get(),LegacySteedRenderer::new);
        event.registerEntityRenderer(Horizons.ENDERSTEED.get(),LegacySteedRenderer::new);
        for(var type:java.util.List.of(Horizons.UNDEAD_HORSE,Horizons.SKELETAL_HORSE))event.registerEntityRenderer(type.get(),InfusedHorseRenderer::new);
        for(var type:java.util.List.of(Horizons.SEAWOLF,Horizons.LUNAR_WOLF,Horizons.NETHER_HOUND))event.registerEntityRenderer(type.get(),InfusedWolfRenderer::new);
        event.registerEntityRenderer(Horizons.SHEEDER.get(),SheederRenderer::new);
        event.registerEntityRenderer(Horizons.TAINT_EATER_PIG.get(),TaintEaterPigRenderer::new);
        event.registerEntityRenderer(Horizons.ORE_PIG.get(),OrePigRenderer::new);
        event.registerEntityRenderer(Horizons.CHOCOLATE_COW.get(),ChocolateCowRenderer::new);
        event.registerEntityRenderer(Horizons.CHROMATIC_SHEEP.get(),net.minecraft.client.renderer.entity.SheepRenderer::new);
        event.registerEntityRenderer(Horizons.SELF_SHEARING_SHEEP.get(),net.minecraft.client.renderer.entity.SheepRenderer::new);
        event.registerEntityRenderer(Horizons.SCHOLAR_CHICKEN.get(),InfusedChickenRenderer::new);
        event.registerEntityRenderer(Horizons.GOLD_CHICKEN.get(),InfusedChickenRenderer::new);
        event.registerBlockEntityRenderer(Horizons.CREATURE_JAR_ENTITY.get(),CreatureJarRenderer::new);
        event.registerBlockEntityRenderer(Horizons.VAT_ENTITY.get(),VatRenderer::new);
        event.registerBlockEntityRenderer(Horizons.VAT_MATRIX_ENTITY.get(),VatMatrixRenderer::new);
        event.registerEntityRenderer(Horizons.SYRINGE_PROJECTILE.get(),SyringeRenderer::new);
        event.registerBlockEntityRenderer(Horizons.BLOOD_INFUSER_ENTITY.get(),BloodInfuserRenderer::new);
        event.registerBlockEntityRenderer(Horizons.NODE_MONITOR_ENTITY.get(),NodeDeviceRenderer::new);
        event.registerBlockEntityRenderer(Horizons.VIS_DYNAMO_ENTITY.get(),NodeDeviceRenderer::new);
        event.registerBlockEntityRenderer(Horizons.ESSENTIA_DYNAMO_ENTITY.get(),NodeDeviceRenderer::new);
        event.registerBlockEntityRenderer(Horizons.SYNTHETIC_NODE_ENTITY.get(),NodeDeviceRenderer::new);
        event.registerBlockEntityRenderer(Horizons.RECOMBINATOR_ENTITY.get(),RecombinatorRenderer::new);
    }
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{ExternalMesh.clear();VortexAudio.clear();});}
}


