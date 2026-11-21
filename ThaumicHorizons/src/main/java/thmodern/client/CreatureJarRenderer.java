package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.entity.LivingEntity;
import thmodern.containment.CreatureJarEntity;

public final class CreatureJarRenderer implements BlockEntityRenderer<CreatureJarEntity> {
    public CreatureJarRenderer(BlockEntityRendererProvider.Context context){}
    public static void creature(LivingEntity subject,PoseStack pose,MultiBufferSource buffers,int light){
        if(subject==null)return;pose.pushPose();pose.translate(.5,.08,.5);
        float scale=Math.min(.5f,Math.min(.52f/Math.max(.1f,subject.getBbHeight()),.44f/Math.max(.1f,subject.getBbWidth())));pose.scale(scale,scale,scale);
        Minecraft.getInstance().getEntityRenderDispatcher().render(subject,0,0,0,0,0,pose,buffers,light);pose.popPose();
    }
    @Override public void render(CreatureJarEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){creature(jar.preview(),pose,buffers,light);}
}
