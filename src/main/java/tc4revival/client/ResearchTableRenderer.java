package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ResearchNotesItem;
import tc4revival.content.ResearchTableBlockEntity;
import tc4revival.content.ScribingToolsItem;
import tc4revival.content.TableBlock;

public  class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity> {
    private final TableModels models = new TableModels();
    private static ResourceLocation texture(String file) { return ResourceLocation.parse("thaumcraft:textures/" + file + ".png"); }
    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ResearchTableBlockEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = table.getBlockState(); var part = state.getValue(TableBlock.PART);
        renderModel(part, state.getValue(TableBlock.FACING), ScribingToolsItem.accepts(table.items.getStackInSlot(0)),
                table.items.getStackInSlot(1), pose, buffers, light, overlay, false);
    }
    public void renderModel(TableBlock.Part part, Direction direction, boolean tools, net.minecraft.world.item.ItemStack note,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderModel(part,direction,tools,note,pose,buffers,light,overlay,true);
    }
    private void renderModel(TableBlock.Part part, Direction direction, boolean tools, net.minecraft.world.item.ItemStack note,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean bodyVisible) {
        if (part == TableBlock.Part.EXTENSION) return;
        pose.pushPose(); pose.translate(0.5, 1, 0.5); pose.mulPose(Axis.XP.rotationDegrees(180));
        if (part == TableBlock.Part.SINGLE) {
            if (direction.getAxis() == Direction.Axis.X) pose.mulPose(Axis.YP.rotationDegrees(90));
            if(bodyVisible)models.single.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture("models/table"))), light, overlay);
        } else {
            float angle = switch (direction) { case NORTH -> 270; case SOUTH -> 90; case WEST -> 180; default -> 0; };
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            var body = buffers.getBuffer(RenderType.entityCutoutNoCull(texture("models/restable")));
            if(bodyVisible)models.research.getChild("body").render(pose, body, light, overlay);
            if (tools) {
                models.research.getChild("inkwell").render(pose, body, light, overlay);
                pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(-90)); pose.mulPose(Axis.XP.rotationDegrees(180));
                pose.translate(-0.17, 0.1, -0.15); pose.mulPose(Axis.YP.rotationDegrees(15)); pose.scale(0.5f, 0.5f, 0.5f);
                quill(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture("blocks/tablequill"))), light, overlay);
                pose.popPose();
            }
            for (int sheet = 0; sheet < 6; sheet++) {
                pose.pushPose(); pose.translate(0.1, -0.01 - sheet * 0.015, 0.35);
                pose.mulPose(Axis.XP.rotationDegrees(-90)); pose.mulPose(Axis.ZP.rotationDegrees(15 + sheet % 3 * 2));
                pose.scale(0.5f, 0.6f, 0.6f);
                quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture("misc/parchment"))), light, overlay,
                        0, 0, 1, 1, 0, 0, 0, 1, 1);
                pose.popPose();
            }
            if (note.getItem() instanceof ResearchNotesItem) {
                var scroll = buffers.getBuffer(RenderType.entityCutoutNoCull(texture("models/restable2")));
                models.scroll.getChild("tube").render(pose, scroll, light, overlay);
                int color = note.hasTag() ? note.getTag().getInt("color") : 0x999999;
                pose.pushPose(); pose.scale(1.2f, 1.2f, 1.2f);
                models.scroll.getChild("ribbon").render(pose, scroll, light, overlay,
                        (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
                pose.popPose();
            }
        }
        pose.popPose();
    }
    static void quad(PoseStack pose, VertexConsumer out, int light, int overlay,
            float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1) {
        vertex(pose, out, light, overlay, x0, y1, z, u0, v1, 0, 0, -1);
        vertex(pose, out, light, overlay, x1, y1, z, u1, v1, 0, 0, -1);
        vertex(pose, out, light, overlay, x1, y0, z, u1, v0, 0, 0, -1);
        vertex(pose, out, light, overlay, x0, y0, z, u0, v0, 0, 0, -1);
    }
    private static void vertex(PoseStack pose, VertexConsumer out, int light, int overlay,
            float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        out.vertex(pose.last().pose(), x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(pose.last().normal(), nx, ny, nz).endVertex();
    }
    private static void quill(PoseStack pose, VertexConsumer out, int light, int overlay) {
        quad(pose, out, light, overlay, 0, 0, 1, 1, 0, 1, 1, 0, 0);
        quad(pose, out, light, overlay, 0, 0, 1, 1, -0.025f, 1, 1, 0, 0);
        // Pixel strips reproduce the old ItemRenderer extrusion without an auxiliary registered item.
        for (int axis = 0; axis < 2; axis++) for (int pixel = 0; pixel < 16; pixel++) for (int side = 0; side < 2; side++) {
            float edge = (pixel + side) / 16f, sample = 1 - (pixel + 0.5f) / 16f;
            for (int corner = 0; corner < 4; corner++) {
                float across = corner == 1 || corner == 2 ? 1 : 0;
                float z = corner >= 2 ? -0.025f : 0;
                vertex(pose, out, light, overlay, axis == 0 ? edge : across, axis == 0 ? across : edge, z,
                        axis == 0 ? sample : 1 - across, axis == 0 ? 1 - across : sample,
                        axis == 0 ? side * 2 - 1 : 0, axis == 1 ? side * 2 - 1 : 0, 0);
            }
        }
    }
}
