package tc4revival.client;

import net.minecraft.client.KeyMapping;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;

@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public  class TCClientRegistration {
    @SubscribeEvent public static void dimensions(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:outer_lands"),new OuterLandsEffects());
    }
    private TCClientRegistration() {}
    @SubscribeEvent public static void bookModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        for (var display:tc4revival.research.ResearchStructureDisplay.values()) if (display.model()!=null)
            event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:item/"+display.model()));
    }
    @SubscribeEvent public static void villageRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.VILLAGER,LegacyVillagerRenderer::new);
        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER,LegacyZombieVillagerRenderer::new);
    }
    @SubscribeEvent public static void taintedEggColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack,layer)->((tc4revival.content.TaintedSpawnEggItem)stack.getItem()).color(layer),tc4revival.content.TCItems.TAINT_HIVE_EGG.get(),tc4revival.content.TCItems.TAINT_SWARM_EGG.get());
        for(var kind:tc4revival.content.TaintedAnimalEntity.Kind.values())event.register((stack,layer)->((tc4revival.content.TaintedSpawnEggItem)stack.getItem()).color(layer),tc4revival.content.TCItems.taintedEgg(kind));
    }
    @SubscribeEvent public static void taintSprites(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        for(int i=1;i<=3;i++)event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/taint_overlay_sprite_"+i));
    }
    @SubscribeEvent public static void eldritchSprites(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        for (String prefix:java.util.List.of("es_","er_")) for (int variant=2;variant<=(prefix.equals("es_") ? 8 : 4);variant++)
            event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/eldritch_texture_"+prefix+variant));
    }
    @SubscribeEvent public static void taintColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state,access,pos,index)-> {
            var world=net.minecraft.client.Minecraft.getInstance().level;
            if(world==null||pos==null)return net.minecraft.world.level.GrassColor.get(.5,1);
            int red=0,green=0,blue=0;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
                int color=world.getBiome(pos.offset(x,0,z)).value().getGrassColor(pos.getX(),pos.getZ());
                red+=color>>16&255;green+=color>>8&255;blue+=color&255;
            }
            return red/9<<16|green/9<<8|blue/9;
        },tc4revival.content.TCArcane.TAINT_FIBRES.get());
        for(int ore=1;ore<7;ore++){final int color=tc4revival.content.ShardType.values()[ore-1].tint();event.register((s,l,p,i)->i==0?color:0xffffff,tc4revival.content.TCArcane.ore(ore),tc4revival.content.TCArcane.deepslateOre(ore));}
        for (int i=0;i<16;i++) { final int color=i; event.register((s,l,p,index)->index==0 ? tc4revival.content.TallowCandleBlock.COLORS[color] : 0xffffff,tc4revival.content.TCArcane.candle(i)); }
        event.register((state,level,pos,index)->0x8899aa,tc4revival.content.TCArcane.SILVERWOOD_LEAVES.get());
        event.register((state,level,pos,index)-> {
            var world=net.minecraft.client.Minecraft.getInstance().level;
            if (level==null || pos==null || world==null) return 4764952;
            int red=0,green=0,blue=0;
            for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
                int color=world.getBiome(pos.offset(x,0,z)).value().getFoliageColor();
                red+=color>>16&255; green+=color>>8&255; blue+=color&255;
            }
            return red/9<<16 | green/9<<8 | blue/9;
        },tc4revival.content.TCArcane.GREATWOOD_LEAVES.get());
        for (var type:tc4revival.content.ShardType.values()) event.register((state,level,pos,index)->
                (type==tc4revival.content.ShardType.BALANCED ? tc4revival.content.ShardType.values()[java.util.concurrent.ThreadLocalRandom.current().nextInt(6)] : type).tint(),tc4revival.content.TCArcane.crystal(type));
        event.register((state,level,pos,index)->level==null || pos==null ? 7160201 : net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level,pos),tc4revival.content.TCArcane.TAINT_SOIL.get());
    }
    @SubscribeEvent public static void finiteFluids(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id,model)->id instanceof net.minecraft.client.resources.model.ModelResourceLocation item
                && item.getNamespace().equals("thaumcraft") && item.getVariant().equals("inventory") ? AnimatedItemExtrusion.wrap(model) : model);
        for(var state:tc4revival.content.TCArcane.TAINT_FIBRES.get().getStateDefinition().getPossibleStates()) {
            var id=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);var original=event.getModels().get(id);
            if(original!=null)event.getModels().put(id,new TaintFibreModel(original));
        }
        var bowId=new net.minecraft.client.resources.model.ModelResourceLocation(net.minecraft.resources.ResourceLocation.parse("thaumcraft:bone_bow"),"inventory");
        // Punchy animates bows using third-person transforms in first person.
        // Keep the vanilla bow parent transforms when its animation renderer is present.
        var bowModel=event.getModels().get(bowId); if (bowModel!=null && !net.minecraftforge.fml.ModList.get().isLoaded("punchy")) event.getModels().put(bowId,new BoneBowModel(bowModel));
        var labelId=new net.minecraft.client.resources.model.ModelResourceLocation(net.minecraft.resources.ResourceLocation.parse("thaumcraft:jar_label"),"inventory");
        var labelModel=event.getModels().get(labelId);if(labelModel!=null)event.getModels().put(labelId,JarLabelRenderer.wrap(labelModel));
        for (var block:java.util.List.of(tc4revival.content.TCEldritch.STONE.get(),tc4revival.content.TCEldritch.NONSPAWNING_STONE.get(),
                tc4revival.content.TCEldritch.ROCK.get(),tc4revival.content.TCEldritch.STAIRS.get(),tc4revival.content.TCEldritch.TRAP.get())) {
            for (var state:block.getStateDefinition().getPossibleStates()) {
                var id=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state); var original=event.getModels().get(id);
                if (original!=null) event.getModels().put(id,new EldritchStoneModel(original,block==tc4revival.content.TCEldritch.ROCK.get(),block==tc4revival.content.TCEldritch.STAIRS.get()));
            }
        }
        for (int i=0;i<16;i++) { var id=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(tc4revival.content.TCArcane.candle(i).defaultBlockState()); var original=event.getModels().get(id); if (original!=null) event.getModels().put(id,new TallowCandleModel(original)); }
        for (var state:tc4revival.content.TCArcane.WARDED_GLASS.get().getStateDefinition().getPossibleStates()) {
            var id=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state); var original=event.getModels().get(id);
            if (original!=null) event.getModels().put(id,new WardedGlassModel(original));
        }
        for (boolean silver:new boolean[]{false,true}) {
            var block=silver ? tc4revival.content.TCArcane.SILVERWOOD_LEAVES.get() : tc4revival.content.TCArcane.GREATWOOD_LEAVES.get();
            var fast=event.getModels().get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/"+(silver ? "silverwood" : "greatwood")+"_leaves_fast"));
            for (var state:block.getStateDefinition().getPossibleStates()) {
                var id=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state); var normal=event.getModels().get(id);
                if (normal!=null && fast!=null) event.getModels().put(id,new MagicalLeavesModel(normal,fast));
            }
        }
        for (var block : java.util.List.of(tc4revival.content.TCArcane.FLUX_GAS.get(),tc4revival.content.TCArcane.FLUX_GOO.get(),tc4revival.content.TCArcane.LIQUID_DEATH.get())) for (var state : block.getStateDefinition().getPossibleStates()) {
            var id = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
            var original = event.getModels().get(id);
            if (original != null) event.getModels().put(id, new FiniteFluxModel(original));
        }
    }
    @SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) { event.register(WandKeys.CHANGE_FOCUS = new KeyMapping("key.thaumcraft.change_focus", org.lwjgl.glfw.GLFW.GLFW_KEY_V, "key.categories.misc")); event.register(WandKeys.CHANGE_AREA = new KeyMapping("key.thaumcraft.change_area", org.lwjgl.glfw.GLFW.GLFW_KEY_B, "key.categories.misc")); event.register(HoverControls.TOGGLE = new KeyMapping("key.thaumcraft.hover", org.lwjgl.glfw.GLFW.GLFW_KEY_H, "key.categories.misc")); }
    @SubscribeEvent public static void extraModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/charger_support"));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/greatwood_leaves_fast"));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/silverwood_leaves_fast"));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:item/pipe_valve_handle"));
        for (String texture:java.util.List.of("pipe_1","pipe_2","pipe_3","pipe_filter","pipe_filter_core","pipe_buffer","pipe_restrict","pipe_oneway")) event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/"+texture));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:item/focus_trade_orn"));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:item/focus_warding_orn"));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:item/focus_hellbat_orn"));
        event.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:item/focus_whatever_orn"));
    }
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(tc4revival.content.TCItems.GOGGLES.get(),GogglesCurioRenderer::new);
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(tc4revival.content.TCItems.SINISTER_STONE.get(),net.minecraft.resources.ResourceLocation.parse("thaumcraft:sinister"),(stack,level,entity,seed)->SinisterStoneView.model(stack)));
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(tc4revival.content.TCItems.ITEM_MIRROR.get(),net.minecraft.resources.ResourceLocation.parse("thaumcraft:linked"),(stack,level,entity,seed)->tc4revival.content.ItemMirrorItem.legacyMetadata(stack)==1?1:0));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.HAND_MIRROR_MENU.get(),HandMirrorScreen::new));
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(tc4revival.content.TCItems.BONE_BOW.get(),
                net.minecraft.resources.ResourceLocation.parse("thaumcraft:bone_pull"),(stack,level,entity,seed)-> {
                    if (entity==null || entity.getUseItem()!=stack) return 0;
                    int ticks=stack.getUseDuration()-entity.getUseItemRemainingTicks(); return ticks>=13?3:ticks>7?2:ticks>0?1:0;
                }));
        event.enqueueWork(()->{
            for (var material:tc4revival.content.GolemMaterial.values()) net.minecraft.client.renderer.item.ItemProperties.register(tc4revival.content.TCItems.golem(material),
                    net.minecraft.resources.ResourceLocation.parse("thaumcraft:golem_configuration"),(stack,level,entity,seed)->stack.hasTag()?(stack.getTag().contains("advanced")?1:0)+(stack.getTag().contains("core")?2:0):0);
        });
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(tc4revival.content.TCItems.ESSENTIA_MIRROR.get(),
                net.minecraft.resources.ResourceLocation.parse("thaumcraft:linked"),(stack,level,entity,seed)->
                        tc4revival.content.EssentiaMirrorItem.legacyMetadata(stack)==7 ? 1 : 0));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.PECH_MENU.get(),PechScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.GOLEM_MENU.get(),GolemScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.MAGIC_BOX_MENU.get(),MagicBoxScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.TRUNK_MENU.get(),TravelingTrunkScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.THAUMATORIUM_MENU.get(),ThaumatoriumScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.ALCHEMY_FURNACE_MENU.get(),AlchemyFurnaceScreen::new));
        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(tc4revival.content.TCFluids.GAS.get(), net.minecraft.client.renderer.RenderType.translucent()));
        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(tc4revival.content.TCFluids.GOO.get(), net.minecraft.client.renderer.RenderType.translucent()));
        event.enqueueWork(()->{
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(tc4revival.content.TCFluids.PURE.get(),net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(tc4revival.content.TCFluids.PURE_FLOWING.get(),net.minecraft.client.renderer.RenderType.translucent());
        });
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.FOCAL_MENU.get(), FocalManipulatorScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.FOCUS_POUCH_MENU.get(), FocusPouchScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.HOVER_HARNESS_MENU.get(), HoverHarnessScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.ARCANE_BORE_MENU.get(), ArcaneBoreScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.ARCANE_SPA_MENU.get(), ArcaneSpaScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.DECONSTRUCTION_TABLE_MENU.get(), DeconstructionTableScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCArcane.MENU.get(), ArcaneWorkbenchScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(tc4revival.content.TCTables.RESEARCH_MENU.get(), ResearchTableScreen::new));
    }
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINT_BOTTLE.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.PRIMAL_ARROW.get(),PrimalArrowRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.LEVITATOR_ENTITY.get(),LevitatorRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ARCANE_LAMP_ENTITY.get(),ArcaneLampRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ARCANE_BORE_BASE_ENTITY.get(),ArcaneBoreBaseRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ARCANE_BORE_ENTITY.get(),ArcaneBoreRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCEldritch.PORTAL_ENTITY.get(),EldritchPortalRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCEldritch.CRYSTAL_ENTITY.get(),EldritchCrystalRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCEldritch.MONUMENT_ENTITY.get(),EldritchMonumentRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCEldritch.LOCK_ENTITY.get(),EldritchLockRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCEldritch.CRAB_SPAWNER_ENTITY.get(),EldritchCrabSpawnerRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCEldritch.VOID_ENTITY.get(),EldritchVoidRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.CULTIST_PORTAL.get(),CultistPortalRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINTACLE.get(),context->new TaintacleRenderer<>(context,10,.6f));
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINTACLE_SMALL.get(),context->new TaintacleRenderer<>(context,6,.2f));
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINTACLE_GIANT.get(),context->new TaintacleRenderer<>(context,14,1));
        event.registerEntityRenderer(tc4revival.content.TCEntities.CULTIST_KNIGHT.get(),CultistRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.CULTIST_CLERIC.get(),CultistRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.CULTIST_LEADER.get(),CultistRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.INHABITED_ZOMBIE.get(),InhabitedZombieRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.BRAINY_ZOMBIE.get(),BrainyZombieRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.GIANT_BRAINY_ZOMBIE.get(),BrainyZombieRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.WISP.get(),WispRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ESSENTIA_TUBE_ENTITY.get(),EssentiaTubeRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.CRYSTAL_CLUSTER_ENTITY.get(),CrystalClusterRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ALUMENTUM.get(),AlumentumRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ALEMBIC_ENTITY.get(),AlembicRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.THAUMATORIUM_ENTITY.get(),ThaumatoriumRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ADVANCED_ALCHEMY_ENTITY.get(),AdvancedAlchemyRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ESSENTIA_RESERVOIR_ENTITY.get(),EssentiaReservoirRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ESSENTIA_MIRROR_ENTITY.get(),EssentiaMirrorRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ITEM_MIRROR_ENTITY.get(),ItemMirrorRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.INFUSION_MATRIX_ENTITY.get(),InfusionMatrixRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ESSENTIA_JAR_ENTITY.get(),EssentiaJarRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.BRAIN_JAR_ENTITY.get(),BrainJarRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.INFUSION_PILLAR_ENTITY.get(),InfusionPillarRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ARCANE_PEDESTAL_ENTITY.get(),ArcanePedestalRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.NODE_JAR_ENTITY.get(),NodeJarRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.PRIMAL_ORB.get(),PrimalOrbRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.FALLING_TAINT.get(),FallingTaintRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINT_SPORE.get(),TaintSporeRenderer::new);
        for(var kind:tc4revival.content.TaintedAnimalEntity.Kind.values())event.registerEntityRenderer(tc4revival.content.TCEntities.taintedAnimal(kind),context->new TaintedAnimalRenderer(context,kind));
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINT_HIVE.get(),TaintHiveRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINT_SWARM.get(),TaintSwarmRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.TAINT_SPIDER.get(),TaintSpiderRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.BELLOWS_ENTITY.get(),BellowsRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.HUNGRY_CHEST_ENTITY.get(),HungryChestRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.MANA_POD_ENTITY.get(),ManaPodRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.CRUCIBLE_ENTITY.get(),CrucibleRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.THAUMIC_SLIME.get(), ThaumicSlimeRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.FIRE_BAT.get(), FireBatRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.PORTABLE_HOLE_ENTITY.get(), PortableHoleRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.WARDED_ENTITY.get(), WardedBlockRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ENERGIZED_NODE_ENTITY.get(), EnergizedNodeRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.AURA_NODE_ENTITY.get(), AuraNodeRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.WAND_PEDESTAL_ENTITY.get(), WandPedestalRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.NODE_STABILIZER_ENTITY.get(), NodeDeviceRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.NODE_CONVERTER_ENTITY.get(), NodeDeviceRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ETHEREAL_BLOOM_ENTITY.get(),EtherealBloomRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ALCHEMY_CENTRIFUGE_ENTITY.get(),AlchemyCentrifugeRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ESSENTIA_CRYSTALLIZER_ENTITY.get(),EssentiaCrystallizerRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.VIS_RELAY_ENTITY.get(), VisRelayRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.WORKBENCH_CHARGER_ENTITY.get(), VisRelayRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.FOCAL_ENTITY.get(), FocalManipulatorRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.SHOCK_ORB.get(), ElectricOrbRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.FROST_SHARD.get(), FrostShardRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.PECH.get(),PechRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ELDRITCH_GOLEM.get(),EldritchGolemRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.GOLEM_ORB.get(),GolemOrbRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ELDRITCH_WARDEN.get(),EldritchWardenRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ELDRITCH_GUARDIAN.get(),EldritchGuardianRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ELDRITCH_ORB.get(),EldritchOrbRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ELDRITCH_CRAB.get(),EldritchCrabRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.MIND_SPIDER.get(),MindSpiderRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.PECH_BLAST.get(), PechBlastRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.EMBER.get(), FireProjectileRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.EXPLOSIVE_ORB.get(), FireProjectileRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.ASPECT_ORB.get(), AspectOrbRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.FLUX_SCRUBBER_ENTITY.get(), FluxScrubberRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.DECONSTRUCTION_TABLE_ENTITY.get(), DeconstructionTableRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCArcane.ENTITY.get(), ArcaneWorkbenchRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.SPECIAL_ITEM.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.GRATE_ITEM.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.FOLLOWING_ITEM.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.GOLEM_BOBBER.get(),GolemBobberRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.GOLEM_DART.get(),GolemDartRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.GOLEM.get(),GolemRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.TRAVELING_TRUNK.get(),TravelingTrunkRenderer::new);
        event.registerEntityRenderer(tc4revival.content.TCEntities.PERMANENT_ITEM.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerBlockEntityRenderer(tc4revival.content.TCTables.TABLE_ENTITY.get(), ResearchTableRenderer::new);
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) BookTextResources::reload);
        event.registerReloadListener(BookItemImages.reloadListener());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> thaumcraft.api.client.ResearchPageRenderers.reload());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> HoverHarnessModel.reload());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) WarpPostEffects::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) RunicHitEffects::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) EssentiaTubeRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) AlembicRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) ThaumatoriumRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) EldritchMeshes::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) AdvancedAlchemyRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) EssentiaReservoirRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) InfusionPillarRenderer::reload);
        event.registerReloadListener(ThaumometerRenderer.instance());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) FluxScrubberRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) EssentiaCrystallizerRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) VisRelayRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) NodeDeviceRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) FrostShardRenderer::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) ConnectedFaces::reload);
    }
    @SubscribeEvent public static void layers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PechModel.LAYER,PechModel::layer);
        event.registerLayerDefinition(FireBatModel.LAYER, FireBatModel::layer);
    }
    @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerBelow(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.PORTAL.id(),"warp_vignette",
                (gui,graphics,partial,width,height)->WarpVignette.render(graphics,width,height));
        event.registerAboveAll("runic_shield",(gui,graphics,partial,width,height)->ClientRunicShield.render(graphics,width,height));
        event.registerAboveAll("sanity_checker",(gui,graphics,partial,width,height)->SanityCheckerHud.render(graphics));
        event.registerAboveAll("wand_vis", (gui, graphics, partial, width, height) -> WandHud.render(graphics, height));
        event.registerAboveAll("hover_fuel", (gui, graphics, partial, width, height) -> HoverControls.render(graphics, height));
        ClientScanFeedback.register(event);
    }
    @SubscribeEvent public static void itemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        for(int ore=1;ore<7;ore++){final int color=tc4revival.content.ShardType.values()[ore-1].tint();event.register((s,i)->i==0?color:0xffffff,tc4revival.content.TCArcane.ore(ore).asItem(),tc4revival.content.TCArcane.deepslateOre(ore).asItem());}
        event.register((stack,index)->index==0?40960:index==1?0xeeccff:0xffffff,tc4revival.content.TCItems.LEVITATOR.get());
        for (int i=0;i<16;i++) { final int color=i; event.register((s,index)->index==0 ? tc4revival.content.TallowCandleBlock.COLORS[color] : 0xffffff,tc4revival.content.TCItems.candle(i)); }
        for (var type : tc4revival.content.ElementalRingType.values())
            event.register((stack, index) -> type.aspect().color(), tc4revival.content.TCItems.elementalRing(type));
        event.register((stack,index)->4764952,tc4revival.content.TCItems.GREATWOOD_LEAVES.get());
        event.register((stack,index)->0x8899aa,tc4revival.content.TCItems.SILVERWOOD_LEAVES.get());
        event.register((stack,layer)->layer==1 ? tc4revival.content.TCItems.ESSENTIA_PHIAL.get().tint(stack) : 0xffffff,tc4revival.content.TCItems.ESSENTIA_PHIAL.get());
        event.register((stack,index)->7160201,tc4revival.content.TCItems.TAINT_SOIL.get());
        event.register((stack,index)->7160201,tc4revival.content.TCItems.TAINT_FIBRES.get(),tc4revival.content.TCItems.TAINT_GRASS.get(),
                tc4revival.content.TCItems.TAINT_PLANT.get(),tc4revival.content.TCItems.TAINT_SPORE_STALK.get(),tc4revival.content.TCItems.TAINT_SPORE_STALK_MATURE.get());
        event.register((stack, layer) -> layer == 0 ? ((tc4revival.content.RobeArmorItem) stack.getItem()).getColor(stack) : 0xffffff,
                tc4revival.content.TCItems.ROBE_CHEST.get(), tc4revival.content.TCItems.ROBE_LEGS.get(), tc4revival.content.TCItems.ROBE_BOOTS.get());
        event.register((stack,layer)->layer==0?((tc4revival.content.VoidRobeArmorItem)stack.getItem()).getColor(stack):0xffffff,
                tc4revival.content.TCItems.VOID_ROBE_HELMET.get(),tc4revival.content.TCItems.VOID_ROBE_CHEST.get(),tc4revival.content.TCItems.VOID_ROBE_LEGS.get());
        event.register((stack, layer) -> tc4revival.content.TCItems.ESSENCE_CRYSTAL.get().tint(stack),tc4revival.content.TCItems.ESSENCE_CRYSTAL.get());
        event.register((stack, layer) -> tc4revival.content.TCItems.WISP_ESSENCE.get().tint(stack), tc4revival.content.TCItems.WISP_ESSENCE.get());
        event.register((stack, layer) -> tc4revival.content.TCItems.MANA_BEAN.get().tint(stack), tc4revival.content.TCItems.MANA_BEAN.get());
        event.register((stack, layer) -> layer != 1 ? 0xffffff : stack.hasTag() ? stack.getTag().getInt("color") : 0x999999,
                tc4revival.content.TCItems.RESEARCH_NOTES.get(), tc4revival.content.TCItems.UNKNOWN_RESEARCH_NOTES.get(),
                tc4revival.content.TCItems.DISCOVERY.get());
    }
}
