package ttmodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MagnetRenderer implements BlockEntityRenderer<MagnetEntity>{
    @SubscribeEvent public static void register(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.MAGNET_ENTITY.get(),context->new MagnetRenderer());}
    @Override public void render(MagnetEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int power=entity.signal();if(power<=0)return;
        float ticks=entity.getLevel().getGameTime()+partial;float half=(power/15f*.7f+.4f)/2;
        float v=((int)(ticks/2)%16)/16f;float spacing=(float)((Math.cos(ticks/10)+1)*.09+.1);
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/item/lightningring.png")));
        float red=entity.pulling()?0:1,blue=entity.pulling()?1:0;
        pose.pushPose();pose.translate(.5,.9,.5);
        for(int i=0;i<3;i++)for(int side=0;side<2;side++){
            int[] order=side==0?new int[]{0,1,2,3}:new int[]{3,2,1,0};
            float[][] points={{-half,-half,0,v},{-half,half,0,v+1/16f},{half,half,1,v+1/16f},{half,-half,1,v}};
            for(int index:order){var p=points[index];out.vertex(pose.last().pose(),p[0],i*spacing,p[1]).color(red,0,blue,.9f).uv(p[2],p[3]).overlayCoords(overlay).uv2(15728880).normal(pose.last().normal(),0,side==0?1:-1,0).endVertex();}
        }
        pose.popPose();
    }
}
