package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** Shared three-dimensional syringe for the infuser and flying injection. */
final class SyringeModel {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/models/syringe.png");
    private final ModelPart root;
    SyringeModel(){
        var mesh=new MeshDefinition();var parts=mesh.getRoot();
        part(parts,"body",0,0,-3,21,-1,9,3,3);
        part(parts,"needle",25,0,-7,22,0,4,1,1);
        part(parts,"plunger",0,8,6,22,0,1,1,1);
        part(parts,"handle",0,12,7,21,-1,1,3,3);
        root=LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    private static void part(PartDefinition root,String name,int u,int v,int x,int y,int z,int w,int h,int d){
        root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(0,0,0,w,h,d),PartPose.offset(x,y,z));
    }
    void render(PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color){
        root.getChild("body").render(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,overlay,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.75f);
        var solid=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for(String name:new String[]{"needle","plunger","handle"})root.getChild(name).render(pose,solid,light,overlay);
    }
}
