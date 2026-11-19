package ttmodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TabletRenderer implements BlockEntityRenderer<TabletEntity>{
    private static final java.util.Map<net.minecraft.client.resources.model.BakedModel,net.minecraft.client.resources.model.BakedModel> MODELS=new java.util.IdentityHashMap<>();
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.ModelEvent.BakingCompleted event){MODELS.clear();}
    @SubscribeEvent public static void register(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.TABLET_ENTITY.get(),context->new TabletRenderer());}
    @Override public void render(TabletEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float time=entity.getLevel().getGameTime()+partial;
        float facing=entity.getBlockState().getValue(TabletBlock.FACING).toYRot();
        plate(pose,buffers,"atCenter",-time,.65f,.130f,overlay);
        plate(pose,buffers,entity.leftClick()?"atLeft":"atRight",time,1,.131f,overlay);
        plate(pose,buffers,"atFacingIndent",-facing,.5f,.132f,overlay);
        var stack=entity.items.getStackInSlot(0);if(stack.isEmpty())return;
        pose.pushPose();pose.translate(.5,.75+Math.cos(time/12)/18,.5);pose.mulPose(Axis.YP.rotationDegrees(-facing+90));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.swing(partial)));pose.mulPose(Axis.XP.rotationDegrees((float)Math.cos(time/8)*5));
        // Legacy RenderItem applied an additional 1/4 block or 1/2 sprite scale.
        float scale=stack.getItem() instanceof net.minecraft.world.item.BlockItem?1f:.6f;pose.scale(scale,scale,scale);
        var renderer=net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        var model=renderer.getModel(stack,entity.getLevel(),null,0);
        if(!(stack.getItem() instanceof net.minecraft.world.item.BlockItem))model=MODELS.computeIfAbsent(model,tc4revival.client.AnimatedItemExtrusion::forDisplay);
        renderer.render(stack,net.minecraft.world.item.ItemDisplayContext.FIXED,false,pose,buffers,light,overlay,model);
        pose.popPose();
    }
    private static void plate(PoseStack pose,MultiBufferSource buffers,String texture,float angle,float size,float y,int overlay){
        pose.pushPose();pose.translate(.5,y,.5);pose.mulPose(Axis.YP.rotationDegrees(angle));float h=size/2;
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse(Tinkerer.ID+":textures/misc/"+texture.toLowerCase(java.util.Locale.ROOT)+".png")));
        float[][] points={{-h,h,0,1},{h,h,1,1},{h,-h,1,0},{-h,-h,0,0}};
        for(var p:points)out.vertex(pose.last().pose(),p[0],0,p[1]).color(1f,1f,1f,1f).uv(p[2],p[3]).overlayCoords(overlay).uv2(15728880).normal(pose.last().normal(),0,1,0).endVertex();
        pose.popPose();
    }
}
