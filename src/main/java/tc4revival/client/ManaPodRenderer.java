package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.ManaPodBlock;
import tc4revival.content.ManaPodBlockEntity;

public  class ManaPodRenderer implements BlockEntityRenderer<ManaPodBlockEntity> {
    private static final ResourceLocation CORE=ResourceLocation.parse("thaumcraft:textures/models/manapod_0.png");
    private static final ResourceLocation SHELL=ResourceLocation.parse("thaumcraft:textures/models/manapod_2.png");
    private final ModelPart core=part(-2,4,5),shell=part(-3.5f,7,9);
    public ManaPodRenderer(BlockEntityRendererProvider.Context context) {}
    private static ModelPart part(float edge,int width,int height) {
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("pod",CubeListBuilder.create().texOffs(0,0).addBox(edge,0,edge,width,height,width),PartPose.ZERO);
        return LayerDefinition.create(mesh,32,32).bakeRoot();
    }
    @Override public void render(ManaPodBlockEntity pod,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        int age=pod.getBlockState().getValue(ManaPodBlock.AGE); if (age<2) return;
        float red=37/255f,green=157/255f,blue=117/255f;
        if (pod.aspect()!=null) {
            int color=pod.aspect().color(); float r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;
            if (age==7) { red=r; green=g; blue=b; }
            else { float weight=age-2; red=(red+r*weight)/(weight+1); green=(green+g*weight)/(weight+1); blue=(blue+b*weight)/(weight+1); }
        }
        pose.pushPose(); pose.translate(.5,.75,.5); pose.mulPose(Axis.XP.rotationDegrees(180));
        int podLight=light;
        if (age>2) {
            var player=Minecraft.getInstance().player;
            float pulse=Mth.sin(((player==null ? 0 : player.tickCount)+pod.hashCode()%100)/8f)*.1f+.9f;
            podLight=age*10+(int)(150*pulse);
            pose.pushPose(); pose.translate(0,.1,0); float scale=.125f*age*pulse; pose.scale(scale,scale,scale);
            core.render(pose,buffers.getBuffer(RenderType.entityTranslucent(CORE)),podLight,overlay,1,1,1,1); pose.popPose();
        }
        float scale=.15f*age; pose.scale(scale,scale,scale);
        shell.render(pose,buffers.getBuffer(RenderType.entityTranslucent(SHELL)),podLight,overlay,red,green,blue,.9f);
        pose.popPose();
    }
}
