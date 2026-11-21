package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ThaumatoriumBlockEntity;

public  class ThaumatoriumRenderer implements BlockEntityRenderer<ThaumatoriumBlockEntity> {
    private static final ResourceLocation MODEL=new ResourceLocation("thaumcraft","models/thaumatorium.obj"),TEXTURE=new ResourceLocation("thaumcraft","textures/models/thaumatorium.png");
    private static GroupedObjMesh mesh;
    public ThaumatoriumRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { mesh=null; }
    @Override public void render(ThaumatoriumBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (entity.upper()) return;
        if (mesh==null) try (var reader=new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open(),java.nio.charset.StandardCharsets.UTF_8)) {
            mesh=GroupedObjMesh.read(reader);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot load thaumatorium model",error); }
        var facing=entity.facing(); pose.pushPose(); pose.translate(.5,0,.5); pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(switch(facing) { case EAST->180; case SOUTH->90; case NORTH->270; default->0; }));
        var frame=pose.last(); var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (var group:mesh.groups().values()) for (var vertex:group)
            out.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(255,255,255,255).uv(vertex.u(),vertex.v())
                    .overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
        pose.popPose();
        var camera=Minecraft.getInstance().getCameraEntity(); var selected=entity.process().selected();
        if (camera==null || selected.isEmpty() || entity.getLevel()==null) return;
        var recipe=entity.recipe(selected.get(Math.floorMod(camera.tickCount/40,selected.size()))); if (recipe.isEmpty()) return;
        var item=recipe.get().getResultItem(entity.getLevel().registryAccess()); item.setCount(1);
        pose.pushPose(); pose.translate(.5+facing.getStepX()/1.99,1.325,.5+facing.getStepZ()/1.99);
        pose.mulPose(Axis.YP.rotationDegrees(switch(facing) { case EAST->90; case WEST->270; case NORTH->180; default->0; }));
        pose.scale(.75f,.75f,.75f);
        Minecraft.getInstance().getItemRenderer().renderStatic(item,net.minecraft.world.item.ItemDisplayContext.FIXED,light,overlay,pose,buffers,entity.getLevel(),0);
        pose.popPose();
    }
}
