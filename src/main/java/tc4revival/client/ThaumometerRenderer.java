package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.TCAspects;

public  class ThaumometerRenderer extends BlockEntityWithoutLevelRenderer {
    private static ThaumometerRenderer instance;
    private static final ResourceLocation MODEL = ResourceLocation.parse("thaumcraft:models/scanner.obj");
    private static final ResourceLocation BODY = ResourceLocation.parse("thaumcraft:textures/models/scanner.png");
    private static final ResourceLocation SCREEN = ResourceLocation.parse("thaumcraft:textures/models/scanscreen.png");
    private ScannerMesh mesh;
    private ThaumometerRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    public static ThaumometerRenderer instance() {
        if (instance == null) instance = new ThaumometerRenderer();
        return instance;
    }
    @Override public void onResourceManagerReload(ResourceManager resources) {
        // External packs may be unavailable while the download failure screen is shown.
        // Match the other external model renderers: reload invalidates, rendering loads.
        mesh = null;
    }
    private void loadMesh(ResourceManager resources) {
        try (var stream = resources.getResourceOrThrow(MODEL).open()) {
            mesh = ScannerMesh.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (java.io.IOException failure) { throw new IllegalStateException("Unable to load the Thaumcraft scanner mesh", failure); }
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (context == ItemDisplayContext.GUI) {
            // Old GUI helpers used ten model units per pixel grid; modern item helpers use sixteen.
            // Include the old INVENTORY_BLOCK helper, with modern GUI Y inversion cancelled.
            pose.scale(0.3125f, -0.3125f, -0.3125f);
            pose.mulPose(Axis.XP.rotationDegrees(210));
            pose.mulPose(Axis.YP.rotationDegrees(-45));
            pose.mulPose(Axis.XP.rotationDegrees(60));
            pose.mulPose(Axis.ZP.rotationDegrees(-30));
            pose.mulPose(Axis.YP.rotationDegrees(-248));
        } else if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            boolean left = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            // Return from ItemInHandLayer's item frame to the animated arm frame.
            pose.translate(left ? 1.0 / 16 : -1.0 / 16, -0.125, 0.625);
            pose.mulPose(Axis.YP.rotationDegrees(-180));
            pose.mulPose(Axis.XP.rotationDegrees(90));
            // Mirror the old right-hand attachment for the modern off hand.
            if (left) pose.scale(-1, 1, 1);
            // Legacy RenderPlayer's BLOCK_3D branch, followed by EQUIPPED_BLOCK.
            pose.translate(-0.0625, 0.625, -0.25);
            pose.mulPose(Axis.XP.rotationDegrees(20));
            pose.mulPose(Axis.YP.rotationDegrees(45));
            pose.scale(-0.375f, -0.375f, 0.375f);
            pose.translate(-0.5, -0.5, -0.5);
            pose.scale(0.5f, 0.5f, 0.5f);
            pose.translate(1.6, 0.3, 2);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(-30));
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // Idle off-hand use goes through vanilla's hand frame; the active two-arm view bypasses this renderer.
            pose.scale(0.18f, 0.18f, 0.18f);
            pose.mulPose(Axis.XP.rotationDegrees(65));
        } else if (context == ItemDisplayContext.FIXED) {
            // ItemFrameRenderer already applies 1/2. Legacy ENTITY + frame helpers total 5/16.
            // The frame's modern facing basis is reversed; add 180 before the old -90 helper.
            // Keep the legacy horizontal mesh instead of flattening it into an upright icon.
            pose.translate(0, -0.035, 0.03125);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.625f, 0.625f, 0.625f);
        } else if (context == ItemDisplayContext.GROUND) {
            // Modern ItemEntityRenderer adds 1/4 block above the shared bob animation.
            pose.translate(0, -0.25, 0);
            // A non-block item uses the old BLOCK_3D helper's 1/2 scale, then TC4's 1/2.
            pose.scale(0.25f, 0.25f, 0.25f);
        } else {
            pose.scale(0.5f, 0.5f, 0.5f);
        }
        renderModel(pose, buffers, light, overlay, false);
        pose.popPose();
    }
    public void renderModel(PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean lens) {
        if (mesh == null) loadMesh(Minecraft.getInstance().getResourceManager());
        var consumer = TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(BODY));
        for (var vertex : mesh.vertices()) vertex(consumer, pose, vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v(),
                vertex.nx(), vertex.ny(), vertex.nz(), 0xffffff, light, overlay);
        pose.pushPose();
        pose.translate(0, 0.11, 0);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        var player = Minecraft.getInstance().player;
        // One continuous oscillation, independent of FPS and the number of scanners being drawn.
        int bright = player == null ? 200 : (int) (200 + Math.sin(player.tickCount + Minecraft.getInstance().getFrameTime()) * 10);
        quad(pose, buffers, SCREEN, -1.25f, -1.25f, 2.5f, 0, 0xffffff, bright, overlay);
        if (lens) {
            // The old centered-quad helper left its scale active for the lens overlay.
            pose.scale(2.5f, 2.5f, 2.5f);
            renderLens(pose, buffers, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, overlay);
        }
        pose.popPose();
    }
    private void renderLens(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var display = ClientLens.display();
        if (display.isEmpty()) return;
        var packet = display.get();
        pose.translate(0, 0, -0.01);
        int column = 0, row = 0, remaining = packet.aspects().size(), halfWidth = Math.min(5, remaining) * 8;
        var mc = Minecraft.getInstance();
        if (!packet.details().getString().isEmpty()) {
            pose.pushPose(); pose.scale(.004f,.004f,.004f);
            mc.font.drawInBatch(packet.details(), -mc.font.width(packet.details()) / 2f, -40, 15642134,
                    false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        for (var value : packet.aspects()) {
            var aspect = TCAspects.catalog().find(value.aspect()).orElse(null);
            pose.pushPose();
            pose.scale(0.0075f, 0.0075f, 0.0075f);
            int x = -halfWidth + column * 16, y = -8 + row * 16;
            quad(pose, buffers, aspect==null?ResourceLocation.parse("thaumcraft:textures/aspects/_empty.png"):thaumcraft.api.client.ClientAspectViews.view(aspect).texture(), x, y, 16, 0.01f, aspect==null?0xdddde8:thaumcraft.api.client.ClientAspectViews.view(aspect).color(), light, overlay);
            if (value.amount() > 0) {
                var number = Integer.toString(value.amount());
                pose.pushPose();
                pose.scale(0.5f, 0.5f, 0.5f);
                mc.font.drawInBatch(number, x * 2 + 32 - mc.font.width(number), y * 2 + 32 - mc.font.lineHeight,
                        0xffffff, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
                pose.popPose();
            }
            pose.popPose();
            if (++column >= 5 - row) {
                column = 0;
                row++;
                remaining -= 5 - row;
                halfWidth = Math.min(6 - row, remaining) * 8;
            }
        }
        String text = packet.name().getString();
        if (text.isEmpty()) return;
        int width = mc.font.width(text);
        float scale = 0.005f - Math.max(0, width - 90) * 0.000025f;
        pose.pushPose();
        pose.translate(0, -0.25, 0);
        pose.scale(scale, scale, scale);
        mc.font.drawInBatch(text, -width / 2f, 0, 0xffffff, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }
    private static void quad(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, float x, float y, float size, float z, int color, int light, int overlay) {
        if (!texture.equals(SCREEN)) {
            var out = TCRenderTypes.buffer(buffers,TCRenderTypes.lens(texture));
            lensVertex(out, pose, x, y + size, z, 0, 1, color, light);
            lensVertex(out, pose, x + size, y + size, z, 1, 1, color, light);
            lensVertex(out, pose, x + size, y, z, 1, 0, color, light);
            lensVertex(out, pose, x, y, z, 0, 0, color, light);
            return;
        }
        var out = TCRenderTypes.buffer(buffers,TCRenderTypes.screen(texture));
        lensVertex(out, pose, x, y + size, z, 0, 1, color, light);
        lensVertex(out, pose, x + size, y + size, z, 1, 1, color, light);
        lensVertex(out, pose, x + size, y, z, 1, 0, color, light);
        lensVertex(out, pose, x, y, z, 0, 0, color, light);
    }
    private static void lensVertex(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v, int color, int light) {
        out.vertex(pose.last().pose(), x, y, z).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255).uv(u, v).uv2(light).endVertex();
    }
    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int color, int light, int overlay) {
        out.vertex(pose.last().pose(), x, y, z).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                .uv(u, v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(), nx, ny, nz).endVertex();
    }
}
