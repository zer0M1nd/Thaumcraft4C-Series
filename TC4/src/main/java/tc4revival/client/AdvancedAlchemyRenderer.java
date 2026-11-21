package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import tc4revival.content.AdvancedAlchemyBlockEntity;

public  class AdvancedAlchemyRenderer implements BlockEntityRenderer<AdvancedAlchemyBlockEntity> {
    private static GroupedObjMesh mesh;
    public AdvancedAlchemyRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { mesh=null; }
    @Override public void render(AdvancedAlchemyBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (!entity.core()) return;
        if (mesh==null) try (var reader=new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(new ResourceLocation("thaumcraft","models/adv_alch_furnace.obj")).open(),java.nio.charset.StandardCharsets.UTF_8)) {
            mesh=GroupedObjMesh.read(reader);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot load advanced alchemy furnace model",error); }
        var process=entity.process(); boolean hot=process.heat()>100,filled=process.amount()>0;
        pose.pushPose(); pose.translate(.5,0,.5); pose.mulPose(Axis.XP.rotationDegrees(-90));
        model("Base","alch_furnace"+(hot ? "_on" : ""),pose,buffers,light,overlay);
        for (int side=0;side<4;side++) {
            pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(90*side));
            model("Tank","alch_furnace_tank"+(filled ? "_on" : ""),pose,buffers,light,overlay); pose.popPose();
        }
        if (filled) {
            pose.pushPose(); pose.translate(.5,-.5,1.1); pose.mulPose(Axis.YP.rotationDegrees(180));
            quad("thaumcraft:block/fluxgoo",190,0,pose,buffers,overlay); pose.popPose();
            float empty=1-process.amount()/500f;
            for (int side=0;side<4;side++) {
                pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(90*side)); pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.translate(.85,-1.8,-1.4); pose.scale(.3f,.6f,1);
                quad("thaumcraft:block/metalbase",150,0,pose,buffers,overlay); pose.translate(0,0,-.01);
                quad("thaumcraft:block/fluxgoo",190,empty,pose,buffers,overlay); pose.popPose();
                pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(-90*side)); pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.translate(1.15,1.8,-1.4); pose.scale(-.3f,-.6f,-1);
                quad("thaumcraft:block/metalbase",150,0,pose,buffers,overlay); pose.translate(0,0,.01);
                quad("thaumcraft:block/fluxgoo",190,empty,pose,buffers,overlay); pose.popPose();
            }
        }
        if (hot) {
            pose.pushPose(); pose.translate(0,0,1);
            for (int side=0;side<4;side++) {
                pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(90*side)); pose.mulPose(Axis.XP.rotationDegrees(135));
                pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.translate(-.5,0,-1);
                quad("minecraft:block/fire_0",220,1-Math.min(1,process.heat()/500f),pose,buffers,overlay);
                pose.translate(0,0,.05); quad("thaumcraft:block/metalbase",150,0,pose,buffers,overlay); pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }
    private static void model(String group,String texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation("thaumcraft","textures/models/"+texture+".png")));
        for (var vertex:mesh.groups().getOrDefault(group,java.util.List.of()))
            out.vertex(pose.last().pose(),vertex.x(),vertex.y(),vertex.z()).color(255,255,255,255).uv(vertex.u(),vertex.v())
                    .overlayCoords(overlay).uv2(light).normal(pose.last().normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
    }
    private static void quad(String texture,int light,float top,PoseStack pose,MultiBufferSource buffers,int overlay) {
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(texture));
        var out=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        for (var point:new float[][]{{0,1,16,16},{1,1,0,16},{1,top,0,0},{0,top,16,0}})
            out.vertex(pose.last().pose(),point[0],point[1],0).color(255,255,255,255).uv(sprite.getU(point[2]),sprite.getV(point[3]))
                    .overlayCoords(overlay).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
    }
}
