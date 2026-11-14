package thaumicexploration.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumicexploration.*;

@Mod.EventBusSubscriber(modid=ExplorationMod.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ExplorationClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(ExplorationMod.THINK_MENU.get(),ThinkScreen::new));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ExplorationMod.MACHINE.get(),MachineRenderer::new);
        event.registerBlockEntityRenderer(ExplorationMod.BOUND.get(),BoundRenderer::new);
        event.registerBlockEntityRenderer(ExplorationMod.CANDLE.get(),context->new CandleRenderer());
        event.registerEntityRenderer(ExplorationMod.MINION.get(),c->new tc4revival.client.TaintacleRenderer<>(c,6,.2F));
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){for(String name:new String[]{"brain","think_tank_brine","jar_fluid","floating_candle"})event.register(ExplorationMod.id("block/"+name));}
    @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item event){
        for(var entry:ExplorationMod.ITEM.values()){
            var item=entry.get();
            if(item instanceof SealItem seal)event.register((stack,tint)->tint==1&&(stack.hasCustomHoverName()||stack.hasTag()&&stack.getTag().contains("network"))?seal.color.getTextColor()^0xffffff:seal.color.getTextColor(),item);
            if(item instanceof BlockItem bi&&bi.getBlock() instanceof FloatingCandle candle)event.register((stack,tint)->candle.color.getTextColor(),item);
        }
    }
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event){
        for(var entry:ExplorationMod.BLOCK.values())if(entry.get() instanceof FloatingCandle candle)event.register((state,level,pos,tint)->candle.color.getTextColor(),candle);
    }
    static void model(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,boolean translucent){
        var client=Minecraft.getInstance();var baked=client.getModelManager().getModel(ExplorationMod.id("block/"+name));
        var layer=translucent?RenderType.translucent():RenderType.cutout();
        client.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(translucent?Sheets.translucentCullBlockSheet():Sheets.cutoutBlockSheet()),null,baked,
            ((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,light,overlay,ModelData.EMPTY,layer);
    }
    static void item(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,Minecraft.getInstance().level,0);}
    public static class CandleRenderer implements BlockEntityRenderer<CandleEntity>{
        @Override public void render(CandleEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();pose.translate(0,FloatingCandle.bob(be.getBlockPos(),be.getLevel().getGameTime()+partial),0);
            model("floating_candle",pose,buffers,light,overlay,((FloatingCandle)be.getBlockState().getBlock()).color.getTextColor(),false);pose.popPose();
        }
    }
    public static class ThinkScreen extends AbstractContainerScreen<ThinkTankMenu>{
        public ThinkScreen(ThinkTankMenu menu,Inventory inventory,Component name){super(menu,inventory,name);}
        @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){
            var texture=ExplorationMod.id("textures/gui/thinktank.png");
            graphics.blit(texture,leftPos,topPos,0,0,imageWidth,imageHeight);
            graphics.blit(texture,leftPos+91,topPos+24,198,11,32,Math.min(28,menu.progress*28/200));
        }
        @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
        @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
    }
    public static class MachineRenderer implements BlockEntityRenderer<MachineEntity>{
        private final BookModel book;
        private static final ResourceLocation BOOK=new ResourceLocation("minecraft","textures/entity/enchanting_table_book.png");
        public MachineRenderer(BlockEntityRendererProvider.Context context){book=new BookModel(context.bakeLayer(ModelLayers.BOOK));}
        @Override public void render(MachineEntity machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            float time=machine.getLevel().getGameTime()+partial;
            if(machine.kind().equals("think_tank")){
                float turn=machine.rotationTicks+partial;
                for(int i=0;i<3;i++){
                    pose.pushPose();pose.translate(.5,.48+Math.sin((time+9.32*i)/14)*.03+machine.warmup/160F,.5);
                    pose.mulPose(Axis.YP.rotationDegrees(turn*2+i*120));pose.translate(0,0,-.266);pose.scale(.3F,.3F,.3F);
                    model("brain",pose,buffers,light,overlay,0xffffff,false);pose.popPose();
                }
                model("think_tank_brine",pose,buffers,light,overlay,0xffffff,true);
                if(machine.warmup>0){
                    int count=machine.space==0?4:6;
                    for(int i=0;i<count;i++){
                        pose.pushPose();pose.translate(.5,.55+Math.sin(time/28)*(i%2==0?.24:-.24),.5);
                        pose.mulPose(Axis.YP.rotationDegrees(-turn*2+360F/count*i));pose.translate(machine.space==0?-1.4:-2,0,0);
                        pose.mulPose(Axis.ZP.rotationDegrees(70));book.setupAnim(time,(turn+i)%7/7F,0,.75F);
                        book.render(pose,buffers.getBuffer(RenderType.entityTranslucent(BOOK)),light,overlay,1,1,1,machine.warmup/40F);pose.popPose();
                    }
                }
            }else if(machine.kind().equals("replicator")){
                var stack=machine.items.getStackInSlot(1).isEmpty()?machine.template:machine.items.getStackInSlot(1);
                if(!stack.isEmpty()){
                    pose.pushPose();pose.translate(.5,1.3+Math.sin(time/10)*.04,.5);pose.mulPose(Axis.YP.rotationDegrees(time*2));pose.scale(.6F,.6F,.6F);item(stack,pose,buffers,light,overlay);pose.popPose();
                }
            }else if(machine.kind().equals("trash_jar")&&machine.amount()>0)fluid(machine.amount(),machine.aspect().color(),pose,buffers,light,overlay);
        }
    }
    static void fluid(int amount,int color,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(0,1/16F,0);pose.scale(1,Math.min(1,amount/64F),1);pose.translate(0,-1/16F,0);model("jar_fluid",pose,buffers,light,overlay,color,true);pose.popPose();}
    public static class BoundRenderer implements BlockEntityRenderer<BoundEntity>{
        private final ModelPart bottom,lid,lock;
        private static final Material CHEST=new Material(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,new ResourceLocation("minecraft","entity/chest/normal"));
        public BoundRenderer(BlockEntityRendererProvider.Context context){var root=context.bakeLayer(ModelLayers.CHEST);bottom=root.getChild("bottom");lid=root.getChild("lid");lock=root.getChild("lock");}
        @Override public void render(BoundEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();
            if(be.isJar()){
                if(be.aspect()!=null&&be.amount()>0)fluid(be.amount(),be.aspect().color(),pose,buffers,light,overlay);
            }else{
                pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(BoundBlock.FACING).toYRot()));pose.translate(-.5,-.5,-.5);
                float open=net.minecraft.util.Mth.lerp(partial,be.previousLid,be.lid);open=1-(1-open)*(1-open)*(1-open);lid.xRot=-(open*(float)Math.PI/2);lock.xRot=lid.xRot;
                var vertices=CHEST.buffer(buffers,RenderType::entityCutout);bottom.render(pose,vertices,light,overlay);lid.render(pose,vertices,light,overlay);lock.render(pose,vertices,light,overlay);
            }
            for(Direction face:Direction.Plane.HORIZONTAL){
                pose.pushPose();pose.translate(.5,be.isJar()?.35:.28,.5);pose.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));pose.translate(0,0,be.isJar()?.316:.442);pose.scale(.22F,.22F,.01F);
                var seal=new ItemStack(ExplorationMod.item((be.isJar()?"jar":"chest")+"_seal_"+DyeColor.byId(be.color).getName()));seal.getOrCreateTag().putString("network",be.network);item(seal,pose,buffers,light,overlay);pose.popPose();
            }
            pose.popPose();
        }
    }
}
