package conciliummodern;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Exact cuboids and UVs exported by Blockbench 5.2.0. */
public final class ConciliumSayaGeometry {
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition saya = partdefinition.addOrReplaceChild("saya", CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -39.5F, -2.0F, 2.0F, 48.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 64);
	}

}
