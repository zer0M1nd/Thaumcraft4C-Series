package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public  class BrainJarRenderer implements BlockEntityRenderer<BrainJarBlockEntity> {
    private static final ResourceLocation BRAIN=ResourceLocation.parse("thaumcraft:textures/models/brain2.png"),BRINE=ResourceLocation.parse("thaumcraft:textures/models/jarbrine.png");
    private static final ModelPart brain=brain(),brine=brine();
    public BrainJarRenderer(BlockEntityRendererProvider.Context context){}
    private static ModelPart brain(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("brain",CubeListBuilder.create().texOffs(0,0).addBox(0,0,0,12,10,16),PartPose.offset(-6,8,-8));
        root.addOrReplaceChild("lower",CubeListBuilder.create().texOffs(64,0).addBox(0,0,0,8,3,7),PartPose.offset(-4,18,0));
        root.addOrReplaceChild("stem",CubeListBuilder.create().texOffs(0,32).addBox(0,0,0,2,6,2),PartPose.offsetAndRotation(-1,18,-2,.4089647f,0,0));
        return LayerDefinition.create(mesh,128,64).bakeRoot();}
    private static ModelPart brine(){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("brine",CubeListBuilder.create().texOffs(0,0).addBox(-4,-11,-4,8,10,8),PartPose.ZERO);return LayerDefinition.create(mesh,64,32).bakeRoot();}
    @Override public void render(BrainJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){contents(jar,partial,pose,buffers,light,overlay);}
    private static void contents(BrainJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var player=Minecraft.getInstance().player;float bob=(float)Math.sin((player==null?0:player.tickCount)/14f)*.03f+.03f;
        pose.pushPose();pose.translate(.5,.01,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.pushPose();pose.translate(0,-.8f+bob,0);
        float delta=jar.rotation-jar.previousRotation;while(delta>=Math.PI)delta-=(float)(2*Math.PI);while(delta<-Math.PI)delta+=(float)(2*Math.PI);
        pose.mulPose(Axis.YP.rotation(jar.previousRotation+delta*partial));pose.mulPose(Axis.YP.rotationDegrees(-90));pose.scale(.4f,.4f,.4f);
        brain.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(BRAIN)),light,overlay);pose.popPose();
        brine.render(pose,buffers.getBuffer(RenderType.entityTranslucent(BRINE)),light,overlay);pose.popPose();
    }
    private static BlockEntityWithoutLevelRenderer item;
    public static BlockEntityWithoutLevelRenderer itemRenderer(){if(item==null)item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
        @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var state=TCArcane.BRAIN_JAR.get().defaultBlockState();var jar=new BrainJarBlockEntity(net.minecraft.core.BlockPos.ZERO,state);pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(90));pose.translate(-.5,0,-.5);contents(jar,0,pose,buffers,light,overlay);pose.popPose();Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
        }};return item;}
}
