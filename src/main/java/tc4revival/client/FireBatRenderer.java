package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.FireBatEntity;

public  class FireBatRenderer extends MobRenderer<FireBatEntity, FireBatModel> {
    private static final ResourceLocation FIRE = ResourceLocation.parse("thaumcraft:textures/models/firebat.png");
    private static final ResourceLocation VAMPIRE = ResourceLocation.parse("thaumcraft:textures/models/vampirebat.png");
    public FireBatRenderer(EntityRendererProvider.Context context) { super(context, new FireBatModel(context.bakeLayer(FireBatModel.LAYER)), .25f); }
    @Override public ResourceLocation getTextureLocation(FireBatEntity bat) { return bat.vampire() ? VAMPIRE : FIRE; }
    @Override protected void scale(FireBatEntity bat, PoseStack pose, float partial) { float scale = bat.devil() || bat.vampire() ? .6f : .35f; pose.scale(scale, scale, scale); }
    @Override protected void setupRotations(FireBatEntity bat, PoseStack pose, float age, float yaw, float partial) {
        pose.translate(0, bat.hanging() ? -.1 : Mth.cos(age * .3f) * .1, 0); super.setupRotations(bat, pose, age, yaw, partial);
    }
    @Override public void render(FireBatEntity bat, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(bat, yaw, partial, pose, buffers, 0xf000f0);
    }
}
