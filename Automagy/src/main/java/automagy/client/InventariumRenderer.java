package automagy.client;

import automagy.content.InventariumBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

public final class InventariumRenderer implements BlockEntityRenderer<InventariumBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("automagy:textures/models/brain_order.png");
    private final ModelPart brain;
    public InventariumRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("brain",CubeListBuilder.create().texOffs(0,0).addBox(-6,8,-8,12,10,16),PartPose.ZERO);
        root.addOrReplaceChild("lower",CubeListBuilder.create().texOffs(64,0).addBox(-4,18,0,8,3,7),PartPose.ZERO);
        root.addOrReplaceChild("stem",CubeListBuilder.create().texOffs(0,32).addBox(0,0,0,2,6,2),PartPose.offsetAndRotation(-1,18,-2,.4089647f,0,0));
        brain=LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    @Override public void render(InventariumBlockEntity core,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float time=core.hasLevel()?(core.getLevel().getGameTime()%10800)+partial:0;
        draw(time,core.hasLevel(),pose,buffers,light,overlay);
    }
    public void draw(float time,boolean animate,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        draw(time,animate,TEXTURE,.4f,pose,buffers,light,overlay);
    }
    public void draw(float time,boolean animate,ResourceLocation texture,float scale,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.ZP.rotationDegrees(180));pose.translate(0,-.9,0);pose.scale(scale,scale,scale);
        if(animate){pose.mulPose(Axis.YP.rotationDegrees(1+time/3));pose.translate(0,-.1+.2*Math.cos(time*Math.PI/200),0);}
        brain.render(pose,buffers.getBuffer(RenderType.entityTranslucent(texture)),light,overlay,1,1,1,.95f);pose.popPose();
    }
}
