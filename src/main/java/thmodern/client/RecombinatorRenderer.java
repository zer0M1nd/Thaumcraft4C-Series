package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import thmodern.node.RecombinatorEntity;

public final class RecombinatorRenderer implements BlockEntityRenderer<RecombinatorEntity>{
    private final ModelPart root;
    public RecombinatorRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var parts=mesh.getRoot();
        cube(parts,"base",0,0,-8,20,-8,16,4,16);
        cube(parts,"pearl",0,20,-2,14,-2,4,4,4);
        cube(parts,"south",0,28,-1,12,7,2,8,1);
        cube(parts,"north",0,28,-1,12,-8,2,8,1);
        cube(parts,"west",6,28,-8,12,-1,1,8,2);
        cube(parts,"east",6,28,7,12,-1,1,8,2);
        cube(parts,"tip",0,48,-2,8,-2,4,2,4);
        cube(parts,"plate",0,38,-4,10,-4,8,2,8);
        cube(parts,"claw_south",0,54,-1,11,4,2,1,4);
        cube(parts,"claw_north",0,54,-1,11,-8,2,1,4);
        cube(parts,"claw_east",0,59,4,11,-1,4,1,2);
        cube(parts,"claw_west",0,59,-8,11,-1,4,1,2);
        root=LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    private static void cube(PartDefinition parent,String name,int u,int v,int x,int y,int z,int width,int height,int depth){
        parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror().addBox(0,0,0,width,height,depth),PartPose.offset(x,y,z));
    }
    @Override public void render(RecombinatorEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,-.5,.5);
        root.getChild("pearl").y=14+(float)Math.sin(entity.animationCount(partial)/8)*2;
        root.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(NodeDeviceRenderer.id("textures/models/recombinator.png"))),light,overlay);
        pose.popPose();
    }
}
