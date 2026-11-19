package tc4revival.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import tc4revival.content.TravelingTrunkEntity;

public  class TravelingTrunkModel extends HierarchicalModel<TravelingTrunkEntity> {
    private final ModelPart root=create().bakeRoot();
    public static LayerDefinition create() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14),PartPose.offset(1,7,15));
        root.addOrReplaceChild("knob",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-15,2,4,1),PartPose.offset(8,7,15));
        root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,19).addBox(0,0,0,14,10,14),PartPose.offset(1,6,1));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public ModelPart root() { return root; }
    public ModelPart knob() { return root.getChild("knob"); }
    @Override public void setupAnim(TravelingTrunkEntity entity,float walk,float amount,float age,float yaw,float pitch) {
        float closed=1-entity.lid(); float rotation=-(1-closed*closed*closed)*3.141593f/2;
        root.getChild("lid").xRot=rotation; knob().xRot=rotation;
    }
}
