package tc4revival.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.FluxScrubberBlockEntity;
public  class FluxScrubberRenderer implements BlockEntityRenderer<FluxScrubberBlockEntity> {
    private static GroupedObjMesh mesh;
    private static BlockEntityWithoutLevelRenderer item;
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/fluxscrubber.png");
    public FluxScrubberRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { mesh=null; }
    private static void model(Direction facing,float ticks,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (mesh==null) try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/obelisk_cap.obj")).open()) { mesh=GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)); } catch(java.io.IOException error) { throw new IllegalStateException("Cannot load flux scrubber",error); }
        pose.pushPose();pose.translate(.5,.5,.5);
        switch(facing) { case DOWN->pose.mulPose(Axis.XP.rotationDegrees(-90));case UP->pose.mulPose(Axis.XP.rotationDegrees(90));case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(Axis.YP.rotationDegrees(90));case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));default->{} }
        pose.translate(0,0,-.5);draw("Cap",pose,buffers,light,overlay);pose.translate(0,0,-(Math.sin(ticks/8)*.075+.075));draw("Tip",pose,buffers,light,overlay);pose.popPose();
    }
    private static void draw(String group,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));var frame=pose.last();
        for(var v:mesh.groups().get(group)) out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(255,255,255,255).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
    private static float ticks() { var camera=Minecraft.getInstance().getCameraEntity(); return camera==null ? 0 : camera.tickCount; }
    @Override public void render(FluxScrubberBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) { model(tile.facing(),ticks()+partial+tile.phase(),pose,buffers,light,overlay); }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if(item==null) item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) { model(Direction.DOWN,ticks(),pose,buffers,light,overlay); }
        }; return item;
    }
}
