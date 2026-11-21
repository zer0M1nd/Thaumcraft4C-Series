package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

public final class HourglassRenderer implements BlockEntityRenderer<HourglassBlockEntity> {
    public HourglassRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(HourglassBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, .2, .5); pose.scale(.2f, .2f, .2f);
        pose.mulPose(Axis.YP.rotationDegrees(90 - tile.getBlockState().getValue(HourglassBlock.FACING).toYRot()));
        float elapsed = Math.min(tile.timer.target(), tile.timer.elapsed() + (tile.timer.running() ? partial : 0));
        float animation = tile.levelTime() - tile.clientFlipTime + partial;
        draw(pose, buffers, light, overlay, tile.getBlockState().is(Content.MAGIC_HOURGLASS.get()),
                elapsed / tile.timer.target(), tile.timer.signal() > 0, animation);
        pose.popPose();
    }
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                            boolean magic, float progress, boolean powered, float animation) {
        part(pose, buffers, light, overlay, magic ? "hourglass-gold" : "hourglass", 1, 1,
                "Right_Pillar_group", "group_Bottom", "group_Left_Pillar", "group_Top");
        if (!magic) part(pose, buffers, light, overlay, "hourglass", 1, 1, "group_Left_Turner", "group_Right_Turner");
        else part(pose, buffers, light, overlay, "hourglass-redstone", powered ? 1 : .5f, .9f, "group_Redstone");
        pose.pushPose();
        boolean rotating = animation >= 0 && animation < 5;
        if (rotating) { pose.mulPose(Axis.XP.rotationDegrees(180)); pose.mulPose(Axis.ZP.rotationDegrees(180*animation/5)); }
        if (progress >= 1) part(pose, buffers, light, overlay, "hourglass-sand", 1, 1, "sandbtm0");
        else {
            int section = Math.min(6, Math.max(1, (int)Math.ceil(progress*6)));
            part(pose, buffers, light, overlay, "hourglass-sand", 1, 1, "sandtop"+section);
            if (section != 1 || !rotating) part(pose, buffers, light, overlay, "hourglass-sand", 1, 1, "sandbtm"+(7-section));
        }
        part(pose, buffers, light, overlay, "hourglass-glass", 1, 1, "group_Glass");
        pose.popPose();
    }
    private static void part(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                             String texture, float shade, float alpha, String... groups) {
        var type = RenderType.entityTranslucent(new ResourceLocation("automagy", "textures/models/"+texture+".png"));
        ExternalObj.HOURGLASS.draw(pose, buffers.getBuffer(type), light, overlay, shade, alpha, groups);
    }
}
