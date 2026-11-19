package conciliummodern;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original ModelWand focus, exported by Blockbench. addBox captured 32x32 UVs before setTextureSize. */
final class ConciliumFocusGeometry {
static LayerDefinition createBodyLayer(){
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition focus = partdefinition.addOrReplaceChild("focus", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	
}
}
