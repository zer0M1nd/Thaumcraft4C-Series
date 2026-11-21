package thaumicfutures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumicfutures.compat.bees.VisAuraProvider;

public final class VisAuraRenderer implements BlockEntityRenderer<VisAuraProvider> {
    public static final ResourceLocation CENTRE = ResourceLocation.parse("thaumicfutures:block/vis_aura_centre");
    public VisAuraRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(VisAuraProvider tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        double tick = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime() + partial;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) (Math.sin(tick * Math.PI * 2 / 80) * .7)));
        pose.mulPose(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(Math.sin((tick + 25) * Math.PI * 2 / 150) * .75),
                (float) Math.sqrt(.5), 0, (float) Math.sqrt(.5)));
        pose.translate(Math.sin(tick * Math.PI * 2 / 1000) * .025, 0, Math.sin((tick + 30) * Math.PI * 2 / 650) * .03);
        float red = (float) (Math.sin(tick * Math.PI * 2 / 130) * .2 + .8);
        float green = (float) (Math.cos((tick + 2) * Math.PI * 2 / 140) * .2 + .8);
        float blue = (float) (Math.sin((tick + 5) * Math.PI * 2 / 99) * .2 + .8);
        var client = Minecraft.getInstance();
        var model = client.getModelManager().getModel(CENTRE);
        client.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.solid()),
                tile.getBlockState(), model, red, green, blue, light, overlay);
        pose.popPose();
    }
}
