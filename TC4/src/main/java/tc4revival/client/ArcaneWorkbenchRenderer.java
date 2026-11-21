package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.ArcaneWorkbenchBlockEntity;

public  class ArcaneWorkbenchRenderer implements BlockEntityRenderer<ArcaneWorkbenchBlockEntity> {
    private static final ModelPart MODEL = create();
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    private static ModelPart create() {
        var mesh = new MeshDefinition();
        var cubes = CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -8, 16, 8, 16)
                .texOffs(0, 32).addBox(-8, 12, -8, 16, 4, 16);
        for (int x : new int[]{-7, 3}) for (int z : new int[]{-7, 3}) cubes.texOffs(72, 0).addBox(x, 8, z, 4, 4, 4);
        mesh.getRoot().addOrReplaceChild("table", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64).bakeRoot();
    }
    public ArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ArcaneWorkbenchBlockEntity table, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderTable(pose, buffers, light, overlay);
        var wand = table.getItem(10);
        if (wand.isEmpty()) return;
        pose.pushPose(); pose.translate(.65, 1.0625, .25); pose.mulPose(Axis.XP.rotationDegrees(90)); pose.mulPose(Axis.ZP.rotationDegrees(20));
        pose.mulPose(Axis.YP.rotationDegrees(-90)); pose.scale(.5f, .5f, .5f);
        pose.translate(0, 1, 0); pose.mulPose(Axis.XP.rotationDegrees(180));
        WandRenderer.instance().renderModel(wand, pose, buffers, light, overlay,
                Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount);
        pose.popPose();
    }
    private static void renderTable(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderTable(pose, buffers, light, overlay, ResourceLocation.parse("thaumcraft:textures/models/worktable.png"));
    }
    static void renderTable(PoseStack pose, MultiBufferSource buffers, int light, int overlay, ResourceLocation texture) {
        pose.pushPose(); pose.translate(.5, 1, .5); pose.mulPose(Axis.XP.rotationDegrees(180));
        MODEL.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer == null) itemRenderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) { renderTable(pose, buffers, light, overlay); }
        };
        return itemRenderer;
    }
}
