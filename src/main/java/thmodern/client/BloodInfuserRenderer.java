package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.*;
import thmodern.Horizons;
import thmodern.blood.BloodInfuserEntity;

public final class BloodInfuserRenderer implements BlockEntityRenderer<BloodInfuserEntity> {
    private final SyringeModel syringe=new SyringeModel();
    public BloodInfuserRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(BloodInfuserEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(entity.isEmpty())return;
        pose.pushPose();pose.translate(1.90625,.75,.46875);pose.mulPose(Axis.ZP.rotationDegrees(90));
        syringe.render(pose,buffers,light,overlay,0xff0000);pose.popPose();
    }
}
