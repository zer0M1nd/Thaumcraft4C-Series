package thmodern.client;

import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.animal.Cat;
import thmodern.Horizons;

/** ModelFamiliar's five hat boxes use unused portions of its own 64x32 skin. */
public class FamiliarModel extends CatModel<Cat> {
    private final ModelPart hat;
    public FamiliarModel(){this(layer().bakeRoot());}
    private FamiliarModel(ModelPart root){super(root);hat=root.getChild("head").getChild("hat");}
    public static LayerDefinition layer(){
        var mesh=CatModel.createBodyMesh(CubeDeformation.NONE);
        mesh.getRoot().getChild("head").addOrReplaceChild("hat",CubeListBuilder.create()
                .texOffs(16,24).addBox(-3,-3,-3.5f,6,1,6)
                .texOffs(48,0).addBox(-1.5f,-5,-2,3,2,3)
                .texOffs(48,8).addBox(-1,-6,-1.5f,2,1,2)
                .texOffs(48,11).addBox(-.5f,-7,-1,1,1,1)
                .texOffs(48,13).addBox(-1,-4,-3,2,1,1),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(Cat cat,float swing,float amount,float age,float yaw,float pitch){
        super.setupAnim(cat,swing,amount,age,yaw,pitch);hat.visible=cat.getType()==Horizons.FAMILIAR.get();
    }
}
