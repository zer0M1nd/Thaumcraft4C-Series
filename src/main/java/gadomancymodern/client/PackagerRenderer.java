package gadomancymodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.automation.PackagerBlock;
import gadomancymodern.automation.PackagerBlockEntity;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

/** Rebuilt model hierarchy: the legacy chassis and separate moving piston keep their own UV atlases. */
public class PackagerRenderer implements BlockEntityRenderer<PackagerBlockEntity> {
    private final ModelPart chassis=chassis(),piston=piston(false),shortPiston=piston(true);
    private static final ResourceLocation BODY=ResourceLocation.parse("gadomancy:textures/models/arcane_packager.png");
    private static final ResourceLocation PISTON=ResourceLocation.parse("gadomancy:textures/models/packager_piston.png");
    public PackagerRenderer(BlockEntityRendererProvider.Context context){}
    private static void part(PartDefinition root,String name,int u,int v,float x,float y,float z,float width,float height,float depth,float rotation){
        root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(0,0,0,width,height,depth),PartPose.offsetAndRotation(x,y,z,0,0,rotation));
    }
    public static ModelPart chassis(){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        part(root,"foot",0,0,-8,21,-8,16,3,16,0);
        part(root,"roof",0,20,-8,12,-8,16,3,16,0);
        part(root,"control",0,40,7,13.5f,-2.5f,1,5,5,0);
        root.addOrReplaceChild("handle",CubeListBuilder.create().texOffs(13,40).addBox(-2,0,0,3,2,2),PartPose.offsetAndRotation(7,15,-1,0,0,(float)Math.PI/4));
        part(root,"handle_pin",13,45,6.3f,16.12f,-1,1,1,2,0);
        part(root,"left",7,40,-8,15,-8,1,6,16,0);
        part(root,"right",24,40,7,15,-8,1,6,16,0);
        part(root,"back_glass",-9,56,-8,15,8,16,6,0,0);
        part(root,"front",6,55,-8,15,-8,16,6,1,0);
        return LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    private static ModelPart piston(boolean cut){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        part(root,"plate",0,0,-5,16,-5,10,2,10,0);
        part(root,"shaft",41,cut?1:0,-2,cut?13:12,-2,4,4,4,0);
        return LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    public static float pistonOffset(float progress){
        if(progress<0)return 0;
        return progress<=40?progress/40f*3/16f:Math.max(0,3/16f-(progress-41)/5f*3/16f);
    }
    @Override public void render(PackagerBlockEntity machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float orientation=switch(machine.getBlockState().getValue(PackagerBlock.FACING)){case NORTH->270;case EAST->180;case SOUTH->90;default->0;};
        renderModel(machine.progress(),orientation,pose,buffers,light,overlay);
    }
    public void renderModel(float progress,float orientation,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.mulPose(Axis.YP.rotationDegrees(orientation));
        float offset=pistonOffset(progress);pose.pushPose();pose.translate(0,offset,0);
        (offset<1/16f?shortPiston:piston).render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(PISTON)),light,overlay);
        pose.popPose();chassis.render(pose,buffers.getBuffer(RenderType.entityTranslucent(BODY)),light,overlay);pose.popPose();
    }
}
