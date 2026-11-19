package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import thmodern.blood.SyringeProjectile;

public final class SyringeRenderer extends EntityRenderer<SyringeProjectile> {
    private final SyringeModel syringe=new SyringeModel();
    public SyringeRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(SyringeProjectile entity){return ResourceLocation.parse("thaumichorizons:textures/models/syringe.png");}
    @Override public void render(SyringeProjectile entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(90+Mth.lerp(partial,entity.yRotO,entity.getYRot())));pose.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(partial,entity.xRotO,entity.getXRot())));
        float shake=entity.shakeTime-partial;if(shake>0)pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake*3)*shake));
        pose.translate(0,-1.36,0);
        syringe.render(pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,net.minecraft.world.item.alchemy.PotionUtils.getColor(entity.getItem()));
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
}
