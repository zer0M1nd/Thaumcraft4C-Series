package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Original strap cube and OBJ backpack, with their independent textures and full-bright machinery. */
public  class HoverHarnessModel extends HumanoidModel<LivingEntity> {
    private static final ResourceLocation OBJ = ResourceLocation.parse("thaumcraft:models/hoverharness.obj");
    private static final ResourceLocation BACK = ResourceLocation.parse("thaumcraft:textures/models/hoverharness2.png");
    private static GroupedObjMesh mesh;
    private LivingEntity wearer;
    public HoverHarnessModel() { super(bake()); }
    private static net.minecraft.client.model.geom.ModelPart bake() {
        var data = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        data.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4,new CubeDeformation(.6f)), net.minecraft.client.model.geom.PartPose.ZERO);
        return LayerDefinition.create(data,64,32).bakeRoot();
    }
    public static void reload() { mesh = null; }
    public void prepare(LivingEntity entity, HumanoidModel<?> original) {
        wearer=entity;
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original, this);
        crouching = entity.isCrouching();
    }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, float r, float g, float b, float alpha) {
        pose.pushPose();
        if (crouching) pose.mulPose(Axis.XP.rotationDegrees(28.64789f));
        // Legacy harness body did not inherit the vanilla sneak translation or arm animations.
        var old = body.storePose(); body.loadPose(net.minecraft.client.model.geom.PartPose.ZERO);
        body.render(pose,out,light,overlay,r,g,b,alpha); body.loadPose(old); pose.popPose();
        if (mesh == null) try (var reader = new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(OBJ).open(), java.nio.charset.StandardCharsets.UTF_8)) {
            mesh = GroupedObjMesh.read(reader);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load hover harness OBJ",failure); }
        pose.pushPose(); pose.scale(.1f,.1f,.1f); pose.mulPose(Axis.XN.rotationDegrees(90));
        if (crouching) pose.mulPose(Axis.XP.rotationDegrees(28.64789f));
        pose.translate(0,.33,-3.7);
        var frame = pose.last(); var vertices = TCRenderTypes.buffer(Minecraft.getInstance().renderBuffers().bufferSource(),RenderType.entityCutoutNoCull(BACK));
        for (var group : mesh.groups().values()) for (var v : group)
            vertices.vertex(frame.pose(),v.x(),v.y(),v.z()).color(r,g,b,alpha).uv(v.u(),v.v()).overlayCoords(overlay).uv2(0xf000f0)
                    .normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
        pose.popPose();
        if (wearer instanceof net.minecraft.world.entity.player.Player player && tc4revival.content.HoverFlight.active(player)) {
            pose.pushPose();
            if (crouching) { pose.mulPose(Axis.XP.rotationDegrees(28.64789f)); pose.translate(0,.075,-.05); }
            pose.translate(0,.2,.55);
            var ring=ResourceLocation.parse("thaumcraft:textures/item/lightningring.png");
            var ringOut=TCRenderTypes.buffer(Minecraft.getInstance().renderBuffers().bufferSource(),TCRenderTypes.runes(ring));
            float frameIndex=(player.level().getGameTime()%16)/16f;
            ClientFocusEffects.quad(ringOut,pose.last().pose(),1.25f,0,frameIndex,1,frameIndex+1/16f,1,1,1,1,230);
            pose.mulPose(Axis.YP.rotationDegrees(180)); pose.translate(0,0,.03);
            ClientFocusEffects.quad(ringOut,pose.last().pose(),.75f,0,frameIndex,1,frameIndex+1/16f,1,.5f,1,1,230);
            pose.popPose(); HoverSparks.emit(player);
        }
    }
}
