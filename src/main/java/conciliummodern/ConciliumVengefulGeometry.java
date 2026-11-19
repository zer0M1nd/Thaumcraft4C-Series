package conciliummodern;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Legacy cuboids and UVs authored and exported in Blockbench 5.2.0. */
public final class ConciliumVengefulGeometry {
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition jar = partdefinition.addOrReplaceChild("jar", CubeListBuilder.create().texOffs(96, 56).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition brain = partdefinition.addOrReplaceChild("brain", CubeListBuilder.create().texOffs(96, 70).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

}
