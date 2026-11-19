package tc4revival.client;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Manually reconstructed geometry from the original three Bore model classes. */
public  class ArcaneBoreModels {
    private ArcaneBoreModels() {}
    private static void part(MeshDefinition mesh,String name,int u,int v,float x,float y,float z,float w,float h,float d,float px,float py,float pz) {
        mesh.getRoot().addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),PartPose.offset(px,py,pz));
    }
    private static ModelPart bake(MeshDefinition mesh) { return LayerDefinition.create(mesh,128,64).bakeRoot(); }
    public static ModelPart base() {
        var mesh=new MeshDefinition();
        part(mesh,"lower",64,24,-8,0,-8,16,2,16,0,0,0);
        part(mesh,"upper",64,24,-8,0,-8,16,2,16,0,14,0);
        part(mesh,"middle",84,42,-2.5f,0,-2.5f,5,12,5,0,2,0);
        int index=0;
        for (int x:new int[]{-5,5}) for (int z:new int[]{-5,5}) part(mesh,"pillar"+index++,64,42,-2,0,-2,4,12,4,x,2,z);
        return bake(mesh);
    }
    public static ModelPart connector() {
        var mesh=new MeshDefinition();
        part(mesh,"stem",106,42,2.5f,-2,-2,5,4,4,0,8,0);
        part(mesh,"end",106,51,7,-2.5f,-2.5f,1,5,5,0,8,0);
        return bake(mesh);
    }
    public static ModelPart carriage() {
        var mesh=new MeshDefinition();
        // setTextureSize occurs after addBox in ModelBore; the baked boxes used 128x64.
        part(mesh,"base",0,32,-6,0,-6,12,2,12,0,0,0);
        part(mesh,"side1",0,0,-2,2,-5.5f,4,8,1,0,0,0);
        part(mesh,"side2",0,0,-2,2,4.5f,4,8,1,0,0,0);
        part(mesh,"crossbar",0,48,-1,-1,-6,2,2,12,0,8,0);
        return bake(mesh);
    }
    public static ModelPart barrel() {
        var mesh=new MeshDefinition();
        part(mesh,"front",30,14,4,-2.5f,-2.5f,4,5,5,0,8,0);
        part(mesh,"middle",0,14,-2,-4,-4,6,8,8,0,8,0);
        return bake(mesh);
    }
    public static ModelPart emitter() {
        var mesh=new MeshDefinition();
        part(mesh,"focus",66,0,-2,12,-2,4,4,4,0,0,0);
        part(mesh,"cross1",56,16,-2,0,-2,4,1,4,0,8,0);
        part(mesh,"cross3",56,16,-2,0,-2,4,1,4,0,0,0);
        part(mesh,"cross2",56,24,-3,4,-3,6,1,6,0,0,0);
        part(mesh,"rod",56,0,-1,1,-1,2,11,2,0,0,0);
        return bake(mesh);
    }
}
