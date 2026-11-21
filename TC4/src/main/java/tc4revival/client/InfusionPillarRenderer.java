package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import tc4revival.content.InfusionPillarBlockEntity;

public  class InfusionPillarRenderer implements BlockEntityRenderer<InfusionPillarBlockEntity> {
    private static final ResourceLocation MODEL=ResourceLocation.parse("thaumcraft:models/pillar.obj");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/pillar.png");
    private static GroupedObjMesh mesh;
    public InfusionPillarRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(ResourceManager resources) { mesh=null; }
    @Override public void render(InfusionPillarBlockEntity pillar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (mesh==null) {
            try (var reader=new InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open(),StandardCharsets.UTF_8)) {
                mesh=GroupedObjMesh.read(reader);
            } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read infusion pillar model",failure); }
        }
        pose.pushPose(); pose.translate(.5,0,.5); pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(switch (pillar.orientation()) { case 3->90; case 4->270; case 5->180; default->0; }));
        var frame=pose.last(); var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (var group:mesh.groups().values()) for (var vertex:group)
            vertices.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(255,255,255,255).uv(vertex.u(),vertex.v())
                    .overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
        pose.popPose();
    }
}
