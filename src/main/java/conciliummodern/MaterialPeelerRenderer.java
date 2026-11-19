package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** Original unit-cube frames and four independently blended shells. */
public final class MaterialPeelerRenderer extends EntityRenderer<MaterialPeeler> {
    public MaterialPeelerRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(MaterialPeeler entity){return ResourceLocation.parse("minecraft:textures/entity/end_portal.png");}
    @Override public void render(MaterialPeeler entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(-.5,0,-.5);
        for(int shell=0;shell<4;shell++){
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(new org.joml.Quaternionf().rotationAxis((entity.innerRotation()+partial)*.5f*(float)Math.PI/180,.7071068f,0,.7071068f));pose.translate(-.5,-.5,-.5);
            if(shell<3){pose.translate(-.1,-.1,-.1);float scale=(float)Math.pow(1.1,shell);pose.scale(scale,scale,scale);}
            var out=buffers.getBuffer(shell<3?ConciliumShaders.endGlow(false):ConciliumShaders.end());
            ConciliumShaders.cube(pose,out);
            pose.popPose();
        }
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
}
