package thmodern.client;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** The legacy 64x32 biped skin, shared by the held effigy and the growing/revived body. */
public class CorpseEffigyModel {
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8)
                .texOffs(32,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(.5f)),PartPose.ZERO);
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4),PartPose.ZERO);
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16).addBox(-3,-2,-2,4,12,4),PartPose.offsetAndRotation(-5,2,0,0,0,.1f));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,12,4),PartPose.offsetAndRotation(5,2,0,0,0,-.1f));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,12,4),PartPose.offset(-1.9f,12,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(1.9f,12,0));
        return LayerDefinition.create(mesh,64,32);
    }
}
