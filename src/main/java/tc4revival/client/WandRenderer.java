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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.WandParts;
import tc4revival.content.WandFocusItem;

/** TC4 model dimensions/UVs and animated runes rebuilt using buffered model rendering. */
public  class WandRenderer extends BlockEntityWithoutLevelRenderer {
    private static WandRenderer instance;
    private final ModelPart cap, bottom, rod, focus;
    private static final ResourceLocation SCRIPT = ResourceLocation.parse("thaumcraft:textures/misc/script.png");
    private WandRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        var mesh = new MeshDefinition(); var root = mesh.getRoot();
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 20, 0));
        root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 8).addBox(-1, -1, -1, 2, 18, 2), PartPose.offset(0, 2, 0));
        root.addOrReplaceChild("focus", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6), PartPose.ZERO);
        // The legacy boxes were constructed while ModelBase was still 32x32;
        // its later setTextureSize calls did not rebuild their UVs.
        var baked = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        cap = baked.getChild("cap"); bottom = baked.getChild("bottom"); rod = baked.getChild("rod"); focus = baked.getChild("focus");
    }
    public static WandRenderer instance() { if (instance == null) instance = new WandRenderer(); return instance; }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var component = WandParts.rod(stack.getTag()); boolean staff = component.staff();
        var mc = Minecraft.getInstance(); var player = mc.player;
        float ticks = player == null ? 0 : player.tickCount;
        boolean first = context.firstPerson();
        pose.pushPose(); pose.translate(.5, .5, .5);
        if (context == ItemDisplayContext.GUI) {
            pose.scale(.625f, -.625f, -.625f);
            pose.mulPose(Axis.XP.rotationDegrees(210)); pose.mulPose(Axis.YP.rotationDegrees(-45));
            if (staff) { pose.translate(0, .5, 0); pose.scale(.8f, .8f, .8f); }
            pose.mulPose(Axis.ZP.rotationDegrees(66)); pose.translate(0, .6, 0);
            if (staff) pose.translate(-.7, .6, 0);
        } else if (context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
            if (context == ItemDisplayContext.GROUND) pose.translate(0, -.25, 0);
            pose.scale(.5f, .5f, .5f);
            if (context == ItemDisplayContext.FIXED) pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.translate(0, staff ? 2 : 1, 0);
            if (staff) pose.scale(.9f, .9f, .9f);
        } else {
            boolean left = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            if (first) { if (left) pose.scale(-1, 1, 1); pose.translate(-.5, -.5, -.5); }
            else {
                pose.translate(left ? 1.0 / 16 : -1.0 / 16, -.125, .625);
                pose.mulPose(Axis.YP.rotationDegrees(-180)); pose.mulPose(Axis.XP.rotationDegrees(90));
                if (left) pose.scale(-1, 1, 1);
                // Old full-3D item attachment and Forge's equipped helper.
                pose.translate(-.0625, .4375, .0625);
                pose.translate(0, .1875, 0); pose.scale(.625f, -.625f, .625f);
                pose.mulPose(Axis.XP.rotationDegrees(-100)); pose.mulPose(Axis.YP.rotationDegrees(45));
                pose.translate(-.5, -.5, -.5);
            }
            if (staff) pose.translate(0, .5, 0);
            pose.translate(.5, 1.5, .5);
            if (first) pose.scale(1, 1.1f, 1);
        }
        pose.mulPose(Axis.XP.rotationDegrees(180));
        boolean equipped = first || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        var holder = first ? player : equipped ? WandRenderContext.holder() : null;
        if (holder instanceof net.minecraft.world.entity.player.Player wielder && wielder.isUsingItem()
                && ItemStack.isSameItemSameTags(wielder.getUseItem(), stack)) {
            float use = wielder.getTicksUsingItem() + mc.getFrameTime();
            pose.translate(0, 1, 0);
            if (first) { pose.mulPose(Axis.XP.rotationDegrees(10)); pose.mulPose(Axis.ZP.rotationDegrees(10)); }
            else pose.mulPose(Axis.ZP.rotationDegrees(33));
            pose.mulPose(Axis.XP.rotationDegrees(-60 * Math.min(3, use) / 3));
            var attached = WandFocusItem.attached(stack);
            boolean wave = stack.hasTag() && stack.getTag().contains("IIUX") || !attached.isEmpty()
                    && ((WandFocusItem) attached.getItem()).animation(attached) == WandFocusItem.Animation.WAVE;
            if (wave) { pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(use / 10) * 10)); pose.mulPose(Axis.XP.rotationDegrees((float) Math.sin(use / 15) * 10)); }
            else if (!attached.isEmpty()) { pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(use / .8f))); pose.mulPose(Axis.XP.rotationDegrees((float) Math.sin(use / .7f))); }
            pose.translate(0, -1, 0);
        }
        renderModel(stack, pose, buffers, light, overlay, ticks);
        pose.popPose();
    }
    public void renderModel(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float ticks) { var mc=Minecraft.getInstance();
        var part = WandParts.rod(stack.getTag()); boolean staff = part.staff(), sceptre = WandParts.sceptre(stack.getTag());
        var capTexture = ResourceLocation.parse(WandParts.cap(stack.getTag()).texture());
        pose.pushPose(); if (staff) pose.translate(0, .2, 0);
        pose.pushPose();
        if (staff) { pose.translate(0, -.1, 0); pose.scale(1.2f, 2, 1.2f); }
        rod.render(pose, TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(ResourceLocation.parse(part.texture()))),
                part.glowing() ? (int) (205 + Math.sin(ticks) * 5) : light, overlay);
        pose.popPose(); pose.pushPose();
        pose.scale(staff ? 1.3f : 1.2f, staff ? 1.1f : 1, staff ? 1.3f : 1.2f);
        var caps = TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(capTexture));
        if (sceptre) {
            pose.pushPose(); pose.scale(1.3f, 1.3f, 1.3f); cap.render(pose, caps, light, overlay); pose.popPose();
            pose.pushPose(); pose.translate(0, .3, 0); pose.scale(1, .66f, 1); cap.render(pose, caps, light, overlay); pose.popPose();
        } else cap.render(pose, caps, light, overlay);
        if (staff) {
            pose.translate(0, .225, 0); pose.pushPose(); pose.scale(1, .66f, 1); cap.render(pose, caps, light, overlay); pose.popPose(); pose.translate(0, .65, 0);
        }
        bottom.render(pose, caps, light, overlay); pose.popPose();
        var attached = WandFocusItem.attached(stack);
        if (!attached.isEmpty()) {
            int color = ((WandFocusItem) attached.getItem()).color(attached);
            String ornament = ((WandFocusItem) attached.getItem()).ornamentTexture(attached);
            if (ornament != null) {
                var model = mc.getModelManager().getModel(ResourceLocation.parse(ornament.contains(":") ? ornament : "thaumcraft:item/" + ornament));
                pose.pushPose(); pose.mulPose(Axis.XP.rotationDegrees(180));
                for (int face = 0; face < 2; face++) {
                    pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(face * 90)); pose.translate(0, .15, .0025); pose.scale(.5f, .5f, .8f);
                    mc.getItemRenderer().render(attached, ItemDisplayContext.NONE, false, pose, buffers, light, overlay, model); pose.popPose();
                }
                pose.popPose();
            }
            String depth = ((WandFocusItem) attached.getItem()).depthTexture(attached);
            if (depth != null) {
                pose.pushPose(); pose.translate(0, staff ? -.15 : -.09, 0);
                pose.scale(staff ? .165f : .16f, staff ? .1765f : .16f, staff ? .165f : .16f);
                focusInterior(pose, buffers, depth, light, overlay); pose.popPose();
            }
            pose.pushPose(); if (staff) pose.translate(0, -.0475, 0);
            pose.scale(staff ? .525f : .5f, staff ? .5525f : .5f, staff ? .525f : .5f);
            var date = java.time.LocalDate.now();
            String texture = date.getMonthValue() == 10 && date.getDayOfMonth() == 31 ? "spec_h" : "wand";
            focus.render(pose, TCRenderTypes.buffer(buffers,RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/models/" + texture + ".png"))),
                    (int) (205 + Math.sin(ticks / 3) * 10), overlay, (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, depth == null ? .95f : .6f);
            pose.popPose();
        }
        if (sceptre) for (int i = 0; i < 10; i++) {
            pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(36 * i + ticks)); rune(pose, buffers, .16, -.01, -.125, i, ticks); pose.popPose();
        }
        if (part.runes()) for (int face = 0; face < 4; face++) {
            pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(90 * (face + 1)));
            for (int i = 0; i < 14; i++) rune(pose, buffers, .36 + i * .14, -.01, -.08, (i + face * 3) % 16, ticks);
            pose.popPose();
        }
        pose.popPose();
    }
    private void rune(PoseStack pose, MultiBufferSource buffers, double x, double y, double z, int index, float ticks) {
        float r = (float) Math.sin((ticks + index * 5) / 5) * .1f + .88f;
        float g = (float) Math.sin((ticks + index * 5) / 7) * .1f + .63f;
        float pulse = (float) Math.sin((ticks + index * 5) / 10) * .2f, size = .06f + pulse / 40;
        pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(90)); pose.translate(x, y, z);
        var matrix = pose.last().pose(); var consumer = TCRenderTypes.buffer(buffers,TCRenderTypes.runes(SCRIPT));
        float low = index / 16f, high = low + 1 / 16f;
        consumer.vertex(matrix, -size, size, 0).color(r, g, .2f, pulse + .6f).uv(high, 1).uv2(200).endVertex();
        consumer.vertex(matrix, size, size, 0).color(r, g, .2f, pulse + .6f).uv(high, 0).uv2(200).endVertex();
        consumer.vertex(matrix, size, -size, 0).color(r, g, .2f, pulse + .6f).uv(low, 0).uv2(200).endVertex();
        consumer.vertex(matrix, -size, -size, 0).color(r, g, .2f, pulse + .6f).uv(low, 1).uv2(200).endVertex();
        pose.popPose();
    }
    private static void focusInterior(PoseStack pose, MultiBufferSource buffers, String texture, int light, int overlay) {
        var out = texture.contains(":")
                ? net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse(texture))
                    .wrap(TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)))
                : TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumcraft:textures/item/" + texture + ".png")));
        var frame = pose.last();
        for (var direction : net.minecraft.core.Direction.values()) {
            var normal = direction.getNormal();
            var n = new net.minecraft.world.phys.Vec3(normal.getX(), normal.getY(), normal.getZ());
            var up = direction.getAxis() == net.minecraft.core.Direction.Axis.Y ? new net.minecraft.world.phys.Vec3(0, 0, 1) : new net.minecraft.world.phys.Vec3(0, 1, 0);
            var right = up.cross(n); var center = n.scale(.5);
            for (int i = 0; i < 4; i++) {
                int sx = i == 0 || i == 1 ? -1 : 1, sy = i == 0 || i == 3 ? -1 : 1;
                var point = center.add(right.scale(sx * .5)).add(up.scale(sy * .5));
                out.vertex(frame.pose(), (float) point.x, (float) point.y, (float) point.z).color(1f, 1f, 1f, 1f)
                        .uv((sx + 1) / 2f, (1 - sy) / 2f).overlayCoords(overlay).uv2(light)
                        .normal(frame.normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
            }
        }
    }
}
