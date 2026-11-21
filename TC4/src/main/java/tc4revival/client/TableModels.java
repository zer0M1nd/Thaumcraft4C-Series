package tc4revival.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Legacy mesh dimensions and UV islands rebuilt with modern model parts. */
 class TableModels {
    final ModelPart single;
    final ModelPart research;
    final ModelPart scroll;
    TableModels() {
        var normal = new MeshDefinition(); var root = normal.getRoot();
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -8, 16, 4, 16), PartPose.ZERO);
        root.addOrReplaceChild("legs", CubeListBuilder.create().texOffs(0, 20).addBox(2, 4, -2, 4, 8, 4)
                .texOffs(0, 20).addBox(-6, 4, -2, 4, 8, 4), PartPose.ZERO);
        root.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(16, 20).addBox(-8, 12, -4, 16, 4, 8), PartPose.ZERO);
        single = LayerDefinition.create(normal, 64, 32).bakeRoot();
        var desk = new MeshDefinition(); root = desk.getRoot();
        var body = CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -8, 32, 4, 16);
        for (float x : new float[] {-6, 18}) for (float z : new float[] {-6, 2}) body.texOffs(0, 24).addBox(x, 4, z, 4, 12, 4);
        body.texOffs(24, 24).addBox(-4, 10, -2, 24, 4, 4);
        root.addOrReplaceChild("body", body, PartPose.ZERO);
        root.addOrReplaceChild("inkwell", CubeListBuilder.create().texOffs(0, 44).addBox(-6, -2, 3, 3, 2, 3), PartPose.ZERO);
        research = LayerDefinition.create(desk, 128, 64).bakeRoot();
        var paper = new MeshDefinition(); root = paper.getRoot();
        root.addOrReplaceChild("tube", CubeListBuilder.create().texOffs(0, 0).addBox(-21, -0.5f, -8, 8, 2, 2),
                PartPose.offsetAndRotation(-2, -2, 2, 0, 10, 0));
        root.addOrReplaceChild("ribbon", CubeListBuilder.create().texOffs(0, 4).addBox(-15.1f, -0.275f, -6.75f, 1, 2, 2),
                PartPose.offsetAndRotation(-2, -2, 2, 0, 10, 0));
        scroll = LayerDefinition.create(paper, 128, 64).bakeRoot();
    }
}
