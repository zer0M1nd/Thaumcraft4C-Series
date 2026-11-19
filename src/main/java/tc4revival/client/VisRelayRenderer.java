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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.ShardType;
import tc4revival.content.TCItems;
import tc4revival.content.VisRelayBlockEntity;

public  class VisRelayRenderer<T extends VisRelayBlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation MODEL = ResourceLocation.parse("thaumcraft:models/vis_relay.obj");
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/models/vis_relay.png");
    private static GroupedObjMesh mesh;
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public VisRelayRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(ResourceManager resources) { mesh = null; }
    @Override public void render(T relay, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        thaumcraft.api.client.DeviceRendering.render(new thaumcraft.api.client.DeviceRendering.Context<>(relay,partial,pose,buffers,light,overlay),()->renderDefault(relay,partial,pose,buffers,light,overlay));
    }
    private void renderDefault(T relay, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var viewer = Minecraft.getInstance().getCameraEntity(); float ticks = (viewer == null ? 0 : viewer.tickCount) + partial;
        int crystalLight = (relay.displayedParentPoint() == null ? 0 : 50) + (int) (150 * (Math.sin(ticks / 2) * .05 + .95));
        renderModel(relay.charger(), relay.orientation(), relay.attunement(), crystalLight, pose, buffers, light, overlay);
        if(relay instanceof tc4revival.content.WorkbenchChargerBlockEntity charger&&charger.extendedSupport()) {
            var mc=Minecraft.getInstance();var support=mc.getModelManager().getModel(ResourceLocation.parse("thaumcraft:block/charger_support"));
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entityCutout(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)),null,support,1,1,1,light,overlay);
        }
        VisRelayBeam.render(relay, ticks, pose, buffers);
    }
    // A connecting beam may enter the frustum while the relay block itself remains outside it.
    @Override public boolean shouldRenderOffScreen(T relay) { return true; }
    private static void renderModel(boolean charger, Direction direction, int color, int crystalLight, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (mesh == null) try (var stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open()) {
            mesh = GroupedObjMesh.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (java.io.IOException error) { throw new IllegalStateException("Unable to load vis relay mesh", error); }
        pose.pushPose(); pose.translate(.5, .5, .5);
        switch (charger ? Direction.DOWN : direction) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            default -> {}
        }
        pose.mulPose(Axis.XP.rotationDegrees(180)); pose.mulPose(Axis.ZP.rotationDegrees(45));
        draw("RingFloat", pose, buffers, light, overlay, 1, 1, 1);
        if (charger) {
            pose.pushPose(); pose.mulPose(Axis.XP.rotationDegrees(180)); pose.translate(0, 0, .5);
            for (int i = 0; i < 4; i++) { draw("Support", pose, buffers, light, overlay, 1, 1, 1); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            pose.popPose();
        } else {
            pose.pushPose(); pose.scale(.75f, .75f, .75f); pose.translate(0, 0, -.16);
            draw("RingBase", pose, buffers, light, overlay, 1, 1, 1); pose.popPose();
        }
        int rgb = color >= 0 && color < 6 ? ShardType.values()[color].tint() : 0xffffff;
        float divisor = color >= 0 ? 200f : 255f;
        draw("Crystal", pose, buffers, crystalLight, overlay, Math.min(1, (rgb >> 16 & 255) / divisor), Math.min(1, (rgb >> 8 & 255) / divisor), Math.min(1, (rgb & 255) / divisor));
        pose.popPose();
    }
    private static void draw(String group, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float red, float green, float blue) {
        var vertices = mesh.groups().get(group); if (vertices == null) return;
        var out = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)); var frame = pose.last();
        for (var v : vertices) out.vertex(frame.pose(), v.x(), v.y(), v.z()).color(red, green, blue, 1)
                .uv(v.u(), v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(), v.nx(), v.ny(), v.nz()).endVertex();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer == null) itemRenderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                boolean charger = stack.is(TCItems.WORKBENCH_CHARGER.get());
                pose.pushPose();
                if (!charger) { pose.translate(.5, .5, .5); pose.scale(1.5f, 1.5f, 1.5f); pose.translate(-.5, -.25, -.5); }
                renderModel(charger, Direction.UP, -1, 150, pose, buffers, light, overlay);
                pose.popPose();
            }
        };
        return itemRenderer;
    }
}
