package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import tc4revival.content.AlembicBlockEntity;
import tc4revival.content.EssentiaTransport;
import tc4revival.content.TCArcane;

public  class AlembicRenderer implements BlockEntityRenderer<AlembicBlockEntity> {
    private static final ResourceLocation MODEL=ResourceLocation.parse("thaumcraft:models/alembic.obj"),TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/alembic.png"),BORE=ResourceLocation.parse("thaumcraft:textures/models/bore.png");
    private static GroupedObjMesh mesh;
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    private final net.minecraft.client.model.geom.ModelPart nozzle;
    public AlembicRenderer(BlockEntityRendererProvider.Context context) {
        var shape=new net.minecraft.client.model.geom.builders.MeshDefinition(); var root=shape.getRoot();
        root.addOrReplaceChild("tube",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(106,42).addBox(2.5f,-2,-2,5,4,4),net.minecraft.client.model.geom.PartPose.offset(0,8,0));
        root.addOrReplaceChild("rim",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(106,51).addBox(7,-2.5f,-2.5f,1,5,5),net.minecraft.client.model.geom.PartPose.offset(0,8,0));
        nozzle=net.minecraft.client.model.geom.builders.LayerDefinition.create(shape,128,64).bakeRoot();
    }
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { mesh=null; }
    private static int rotation(Direction side) { return switch(side) { case EAST->180; case SOUTH->90; case NORTH->270; default->0; }; }
    @Override public void render(AlembicBlockEntity alembic,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (mesh==null) try (var reader=new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open(),java.nio.charset.StandardCharsets.UTF_8)) { mesh=GroupedObjMesh.read(reader); }
        catch (java.io.IOException error) { throw new IllegalStateException("Cannot read alembic model",error); }
        pose.pushPose(); pose.translate(.5,0,.5); pose.pushPose(); pose.mulPose(Axis.XP.rotationDegrees(-90));
        if (alembic.getLevel()==null) { pose.translate(0,0,-.4); draw("Legs",pose,buffers,light,overlay); }
        else {
            pose.mulPose(Axis.ZP.rotationDegrees(rotation(alembic.facing())));
            var below=alembic.getLevel().getBlockState(alembic.getBlockPos().below());
            if (below.is(TCArcane.ALCHEMY_FURNACE.get()) || below.is(TCArcane.ALEMBIC.get())) draw("TubeMain",pose,buffers,light,overlay);
            draw(below.is(TCArcane.ALEMBIC.get()) ? "TubeSmall" : "Legs",pose,buffers,light,overlay);
        }
        draw("Pot",pose,buffers,light,overlay); draw("Panel",pose,buffers,light,overlay); pose.popPose();
        if (alembic.filter()!=null) {
            pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(90+rotation(alembic.facing()))); pose.translate(0,.468,-.409);
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.softTranslucent(ResourceLocation.parse("thaumcraft:textures/models/label.png"))),pose.last().pose(),.135f,1,0,0,1,1,1,1,1,light);
            pose.translate(0,0,-.001); pose.mulPose(Axis.ZP.rotationDegrees(180));
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.softTranslucent(alembic.filter().texture())),pose.last().pose(),.104f,1,1,0,0,.1f,.1f,.1f,.8f,light); pose.popPose();
        }
        pose.popPose();
        if (alembic.getLevel()!=null) for (var side:Direction.values()) {
            var pos=alembic.getBlockPos().relative(side);
            if (alembic.getLevel().hasChunkAt(pos) && alembic.getLevel().getBlockEntity(pos) instanceof tc4revival.content.EssentiaTubeBlockEntity tube && tube.flow().kind()!=tc4revival.content.EssentiaTubeFlow.Kind.BUFFER) continue;
            if (!alembic.canOutput(side) || !alembic.getLevel().hasChunkAt(pos) || !(alembic.getLevel().getBlockEntity(pos) instanceof EssentiaTransport other) || !other.connectable(side.getOpposite())) continue;
            pose.pushPose(); pose.translate(.5,0,.5);
            if (side==Direction.UP) { pose.translate(.5,.5,0); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            else pose.mulPose(Axis.YP.rotationDegrees(switch(side) { case NORTH->90; case SOUTH->270; case WEST->180; default->0; }));
            nozzle.render(pose,TCRenderTypes.buffer(buffers,RenderType.entitySolid(BORE)),light,overlay); pose.popPose();
        }
    }
    private static void draw(String group,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var out=TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(TEXTURE)); var frame=pose.last();
        for (var vertex:mesh.groups().getOrDefault(group,java.util.List.of())) out.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(255,255,255,255)
                .uv(vertex.u(),vertex.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            final AlembicRenderer renderer=new AlembicRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                // Old BlockMetalDeviceRenderer centers X/Z only, leaving Y at zero.
                // Modern ItemRenderer has already subtracted .5 from all three axes.
                pose.pushPose();pose.translate(0,.5,0);
                renderer.render(new AlembicBlockEntity(net.minecraft.core.BlockPos.ZERO,TCArcane.ALEMBIC.get().defaultBlockState()),0,pose,buffers,light,overlay);
                pose.popPose();
            }
        };
        return itemRenderer;
    }
}
