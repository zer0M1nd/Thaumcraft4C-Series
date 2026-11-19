package wgmodern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class WGClient {
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)m->{GemRenderer.clear();LegacyObj.clear();PhotoRenderer.clear();ClusterColors.clear();});}
    public static final ModelLayerLocation WHEEL_LAYER=new ModelLayerLocation(new ResourceLocation("witchinggadgets","spinning_wheel"),"main");
    @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent e){e.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HELMET.id(),"sauna",(gui,g,partial,w,h)->WGEffects.renderSteam(g,w,h));}
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e){e.register((s,l,p,t)->l!=null&&p!=null&&l.getBlockEntity(p) instanceof EtherealWall.Tile tile&&tile.camouflage!=null?Minecraft.getInstance().getBlockColors().getColor(tile.camouflage,l,p,t):-1,Content.ETHEREAL_WALL.get());}
    @SubscribeEvent public static void bakedModels(ModelEvent.ModifyBakingResult e){for(var entry:e.getModels().entrySet())if(entry.getKey().getNamespace().equals("witchinggadgets")&&entry.getKey().getPath().equals("ethereal_wall"))entry.setValue(new EtherealWallModel(entry.getValue()));}
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{ResearchRecipeDisplay.setup();net.minecraft.client.renderer.item.ItemProperties.register(Content.INFUSED_GEM.get(),new ResourceLocation("witchinggadgets","gem_cut"),(s,l,p,seed)->InfusedGem.cut(s));MenuScreens.register(Content.LOCK_MENU.get(),TileLockClient.Screen::new);Metallurgy.CLUSTERS.values().forEach(i->net.minecraft.client.renderer.item.ItemProperties.register(i.get(),new ResourceLocation("witchinggadgets","cluster_type"),(s,l,p,seed)->ClusterColors.get(((Metallurgy.Cluster)s.getItem()).metal)[1]));MenuScreens.register(Content.EQUIPPED_POUCH_MENU.get(),EquippedPouchScreen::new);MenuScreens.register(Content.LABEL_MENU.get(),LabelScreen::new);MenuScreens.register(Content.CUTTING_MENU.get(),CuttingScreen::new);MenuScreens.register(Content.SPINNING_MENU.get(),SpinningScreen::new);MenuScreens.register(Content.BAG_MENU.get(),BagScreen::new);MenuScreens.register(Content.CLOAK_MENU.get(),CloakScreen::new);Content.CLOAKS.values().forEach(item->top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(item.get(),CloakRenderer::new));Content.GEAR.values().forEach(item->top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(item.get(),AccessoryRenderer::new));});}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){e.register((s,t)->InfusedGem.color(s),Content.INFUSED_GEM.get());e.register((s,t)->t==0?ClusterColors.get(((Metallurgy.Cluster)s.getItem()).metal)[0]:-1,Metallurgy.CLUSTERS.values().stream().map(x->x.get()).toArray(Item[]::new));e.register(new net.minecraftforge.client.model.DynamicFluidContainerModel.Colors(),Content.CAPSULE.get());Content.CLOAKS.values().forEach(item->e.register((stack,tint)->tint==0?((CloakItem)stack.getItem()).getColor(stack):-1,item.get()));e.register((stack,tint)->tint==0?((AdvancedRobe)stack.getItem()).getColor(stack):-1,Content.ROBE_CHEST.get(),Content.ROBE_LEGS.get());e.register((stack,tint)->tint==0?((BagItem)stack.getItem()).getColor(stack):-1,Content.BAG.get(),Content.VOID_BAG.get(),Content.ENDER_BAG.get(),Content.HUNGRY_BAG.get());for(var entry:Content.MATERIALS.entrySet())if(entry.getKey().startsWith("cloth_")){String aspect=switch(entry.getKey()){case "cloth_space"->"vacuos";case "cloth_golden"->"lucrum";default->"praecantatio";};e.register((s,i)->thaumcraft.api.aspects.Aspect.getAspect(aspect).getColor(),entry.getValue().get());}}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(WHEEL_LAYER,WGClient::wheelLayer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Content.LOCK_TILE.get(),TileLockClient.Renderer::new);e.registerBlockEntityRenderer(Content.SARCOPHAGUS_TILE.get(),SarcophagusRenderer::new);e.registerBlockEntityRenderer(Content.TERRAFORMER_TILE.get(),TerraformRenderer::new);e.registerBlockEntityRenderer(Content.TERRAFORM_FOCUS_TILE.get(),TerraformRenderer.FocusRenderer::new);e.registerBlockEntityRenderer(Content.PUMP_TILE.get(),PumpRenderer::new);e.registerBlockEntityRenderer(Content.LABEL_TILE.get(),LabelRenderer::new);e.registerBlockEntityRenderer(Content.CUTTING_TILE.get(),CuttingRenderer::new);e.registerBlockEntityRenderer(Content.SPINNING_TILE.get(),WheelRenderer::new);e.registerBlockEntityRenderer(Content.EXTRUDER_TILE.get(),ExtruderRenderer::new);}
    public static class SpinningScreen extends AbstractContainerScreen<SpinningMenu>{
        private static final ResourceLocation TEXTURE=new ResourceLocation("witchinggadgets","textures/gui/spinningwheel.png");
        public SpinningScreen(SpinningMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=184;imageHeight=233;}
        @Override protected void renderBg(GuiGraphics g,float pt,int mx,int my){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos+144,0,144,184,90);tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,38,144);tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+126,topPos,126,0,48,144);int width=(int)Math.ceil(88d*menu.data.get(0)/Math.max(1,menu.data.get(1)));if(width>0)tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+38,topPos,38,0,Math.min(88,width),144);}
        @Override protected void renderLabels(GuiGraphics g,int mx,int my){}
        @Override public void render(GuiGraphics g,int x,int y,float pt){renderBackground(g);super.render(g,x,y,pt);renderTooltip(g,x,y);}
    }
    public static class BagScreen extends AbstractContainerScreen<BagMenu>{
        private final ResourceLocation texture;
        public BagScreen(BagMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;texture=new ResourceLocation("witchinggadgets","textures/gui/"+(menu.filter?"voidbag":"bag")+".png");}
        @Override protected void renderBg(GuiGraphics g,float pt,int mx,int my){tc4revival.client.LegacyGui.blit(g,texture,leftPos,topPos,0,0,imageWidth,imageHeight);}
        @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,Component.translatable("container.inventory"),4,74,4210752,false);}
        @Override public void render(GuiGraphics g,int x,int y,float pt){renderBackground(g);super.render(g,x,y,pt);renderTooltip(g,x,y);}
    }
    public static class CloakScreen extends AbstractContainerScreen<CloakMenu>{
        private static final ResourceLocation TEXTURE=new ResourceLocation("witchinggadgets","textures/gui/bagcloak.png");
        public CloakScreen(CloakMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=166;}
        @Override protected void renderBg(GuiGraphics g,float pt,int x,int y){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,176,166);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,Component.translatable("container.inventory"),4,74,4210752,false);}
        @Override public void render(GuiGraphics g,int x,int y,float pt){renderBackground(g);super.render(g,x,y,pt);renderTooltip(g,x,y);}
    }
    /** Authored ModelPart implementation of the original 64x32 model's 17 boxes and UV layout. */
    public static LayerDefinition wheelLayer(){var mesh=new MeshDefinition();var r=mesh.getRoot();
        box(r,"leg1",0,0,0,0,0,2,8,2,1,0,14,0,0);box(r,"leg2",0,0,0,0,0,2,8,2,-3,0,14,0,0);box(r,"leg3",0,0,0,0,0,2,6,2,-1,0,1,0,0);
        box(r,"base",0,0,0,0,0,6,2,16,-3,5,.25f,-10,0);
        box(r,"wheel1",8,0,0,0,0,1,3,1,-.5f,12,0,0,0);box(r,"wheel2",8,0,0,0,0,1,3,1,-.5f,12,6.35f,0,0);
        box(r,"wheel3",8,4,0,0,0,1,1,3,-.5f,9.9f,2.2f,0,0);box(r,"wheel4",8,4,0,0,0,1,1,3,-.5f,16.1f,2.2f,0,0);
        box(r,"diagonal1",8,4,0,0,-1.5f,1,1,3,-.5f,15.3f,5.6f,45,0);box(r,"diagonal2",8,4,0,0,-1.5f,1,1,3,-.5f,11,6.3f,-45,0);box(r,"diagonal3",8,4,0,0,-1.5f,1,1,3,-.5f,15.3f,1.8f,-45,0);box(r,"diagonal4",8,4,0,0,-1.5f,1,1,3,-.5f,11,1.1f,45,0);
        box(r,"cross1",28,0,0,13,3.7f,1,6,1,-.5f,-2.5f,-.5f,0,0);box(r,"cross2",28,8,0,13.6f,3,1,1,6,-.5f,-.5f,-2.5f,0,0);
        box(r,"support",36,0,5,10,3.7f,1,7,2,-.5f,-4,-1,0,15);box(r,"spindle",28,0,0,9,13,1,4,1,-.5f,0,-.5f,0,0);box(r,"thread",0,18,0,11,13,2,3,2,-1,0,-1,0,0);
        return LayerDefinition.create(mesh,64,32);
    }
    private static void box(PartDefinition root,String name,int u,int v,float x,float y,float z,int sx,int sy,int sz,float px,float py,float pz,float rx,float rz){root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,sx,sy,sz),PartPose.offsetAndRotation(px,py,pz,(float)Math.toRadians(rx),0,(float)Math.toRadians(rz)));}
    public static class WheelRenderer implements BlockEntityRenderer<SpinningWheel.Tile>{
        private final ModelPart model;private static final ResourceLocation TEXTURE=new ResourceLocation("witchinggadgets","textures/models/spinningwheel.png");
        public WheelRenderer(BlockEntityRendererProvider.Context c){model=c.bakeLayer(WHEEL_LAYER);}
        @Override public void render(SpinningWheel.Tile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(model,tile.getBlockState().getValue(SpinningWheel.FACING),pose,buffers,light,overlay);}
        public static void draw(ModelPart model,Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();switch(facing){case NORTH->{pose.mulPose(Axis.YP.rotationDegrees(90));pose.translate(-.5,0,0);}case SOUTH->{pose.mulPose(Axis.YP.rotationDegrees(270));pose.translate(.5,0,-1);}case WEST->{pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(-.5,0,-1);}default->pose.translate(.5,0,0);}model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay);pose.popPose();}
    }
    public static class ItemRenderer extends BlockEntityWithoutLevelRenderer{
        private ModelPart model;
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager){super.onResourceManagerReload(manager);model=null;}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(stack.is(Content.PUMP_ITEM.get())){PumpRenderer.render(Direction.NORTH,pose,buffers,light,overlay);return;}if(stack.is(Content.LABEL_ITEM.get())){LabelRenderer.draw(null,Direction.NORTH,pose,buffers,light,overlay);return;}if(stack.is(Content.CUTTING_ITEM.get())){CuttingRenderer.draw(null,Direction.NORTH,pose,buffers,light,overlay);return;}if(stack.is(Content.EXTRUDER_ITEM.get())){ExtruderRenderer.draw(Direction.NORTH,0,true,pose,buffers,light,overlay);return;}if(model==null)model=Minecraft.getInstance().getEntityModels().bakeLayer(WHEEL_LAYER);WheelRenderer.draw(model,Direction.EAST,pose,buffers,light,overlay);}
    }
}
