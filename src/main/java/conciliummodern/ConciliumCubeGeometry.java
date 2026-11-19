package conciliummodern;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original ModelCube geometry and 64x32 UV net, exported by Blockbench. */
final class ConciliumCubeGeometry {
static LayerDefinition createBodyLayer(){
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition cube = partdefinition.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 8.0F, 8.0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	
}
}
