package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.content.NodeConverterBlockEntity;
import tc4revival.content.NodeStabilizerBlockEntity;
import tc4revival.content.TCItems;

public  class NodeDeviceRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation MODEL = ResourceLocation.parse("thaumcraft:models/node_stabilizer.obj");
    private static final ResourceLocation BUBBLE = ResourceLocation.parse("thaumcraft:textures/misc/node_bubble.png");
    private static GroupedObjMesh mesh;
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public NodeDeviceRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(ResourceManager resources) { mesh = null; }
    @Override public void render(T block, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean converter = block instanceof NodeConverterBlockEntity;
        int count = converter ? ((NodeConverterBlockEntity) block).count() : ((NodeStabilizerBlockEntity) block).extension();
        int mode = converter ? ((NodeConverterBlockEntity) block).status() : ((NodeStabilizerBlockEntity) block).lockStrength();
        var camera = Minecraft.getInstance().getCameraEntity(); float ticks = (camera == null ? 0 : camera.tickCount) + partial;
        renderModel(converter, mode, count, ticks, pose, buffers, light, overlay);
    }
    private static void renderModel(boolean converter, int mode, int count, float ticks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (mesh == null) try (var stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open()) {
            mesh = GroupedObjMesh.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (java.io.IOException error) { throw new IllegalStateException("Unable to load node device model", error); }
        String name = converter ? "node_converter" : "node_stabilizer";
        var texture = ResourceLocation.parse("thaumcraft:textures/models/" + name + ".png");
        var glowing = ResourceLocation.parse("thaumcraft:textures/models/" + name + "_over.png");
        float extension = converter ? Math.max(0, Math.min(50, count)) / 137f : count / 100f;
        int color = converter ? mode == 2 ? 0xff004c : mode == 1 ? 0xff9919 : 0x7fff7f : mode == 2 ? 0xff3333 : 0xffffff;
        pose.pushPose(); pose.translate(.5, converter ? 1 : 0, .5); pose.mulPose(Axis.XP.rotationDegrees(converter ? 90 : -90));
        draw("lock", texture, pose, buffers, light, overlay, 0xffffff);
        if (converter) draw("lock", glowing, pose, buffers, glow(ticks, extension * 2.5f), overlay, color);
        for (int i = 0; i < 4; i++) {
            pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(i * 90)); pose.mulPose(Axis.YP.rotationDegrees(45)); pose.translate(0, 0, extension);
            draw("piston", texture, pose, buffers, light, overlay, 0xffffff);
            draw("piston", glowing, pose, buffers, glow(ticks + i * 5, converter ? extension * 2.5f : count / 37f), overlay, color);
            pose.popPose();
        }
        pose.popPose();
        if (!converter && count > 0) {
            pose.pushPose(); pose.translate(.5, 1.5, .5); pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            float alpha = count / 37f * ((float) Math.sin(ticks / 8) * .1f + .5f);
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.runes(BUBBLE)), pose.last().pose(), .9f, 0, 0, 1, 1, 1, mode == 2 ? 68 / 255f : 1, mode == 2 ? 68 / 255f : 1, alpha, 220);
            pose.popPose();
        }
    }
    private static int glow(float ticks, float extension) { return 50 + (int) (170 * extension * ((float) Math.sin(ticks / 3) * .1f + .9f)); }
    private static void draw(String group, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int color) {
        var vertices = mesh.groups().get(group); if (vertices == null) return;
        var out = TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(texture)); var frame = pose.last();
        for (var v : vertices) out.vertex(frame.pose(), v.x(), v.y(), v.z()).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                .uv(v.u(), v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(), v.nx(), v.ny(), v.nz()).endVertex();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer == null) itemRenderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                renderModel(stack.is(TCItems.NODE_CONVERTER.get()), stack.is(TCItems.ADVANCED_NODE_STABILIZER.get()) ? 2 : 1, 0, 0, pose, buffers, light, overlay);
            }
        };
        return itemRenderer;
    }
}
