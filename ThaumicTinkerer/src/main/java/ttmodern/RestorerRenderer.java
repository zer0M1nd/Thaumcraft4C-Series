package ttmodern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class RestorerRenderer implements BlockEntityRenderer<RestorerEntity> {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.RESTORER_ENTITY.get(),context->new RestorerRenderer());}
    @Override public void render(RestorerEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        float rotation=entity.getLevel()==null?0:(entity.getLevel().getGameTime()+partial)*.75f%360;
        pose.pushPose();pose.translate(.5,.975,.5);pose.mulPose(Axis.YP.rotationDegrees(rotation));
        var texture=net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":textures/misc/"+(entity.repairing()?"repair":"repair_off")+".png");
        var out=buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture));
        float half=.625f;float[][] vertices={{-half,half,0,1},{half,half,1,1},{half,-half,1,0},{-half,-half,0,0}};
        for(var vertex:vertices)out.vertex(pose.last().pose(),vertex[0],0,vertex[1]).color(255,255,255,255).uv(vertex[2],vertex[3]).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),0,1,0).endVertex();
        pose.popPose();
        var stack=entity.inventory.getStackInSlot(0);if(stack.isEmpty())return;
        float time=entity.getLevel()==null?0:entity.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,.625+Math.sin(time/10)*.05,.5);
        pose.mulPose(Axis.YP.rotationDegrees(time*.75f%360));pose.scale(.5f,.5f,.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,entity.getLevel(),0);
        pose.popPose();
    }
}
