package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EssentiaReservoirBlockEntity;
import tc4revival.content.TCArcane;

public  class EssentiaReservoirRenderer implements BlockEntityRenderer<EssentiaReservoirBlockEntity> {
    private static final ResourceLocation MODEL=ResourceLocation.parse("thaumcraft:models/reservoir.obj");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/reservoir.png");
    private static GroupedObjMesh mesh;
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public EssentiaReservoirRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { mesh=null; }
    @Override public void render(EssentiaReservoirBlockEntity reservoir,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (mesh==null) try (var reader=new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open(),java.nio.charset.StandardCharsets.UTF_8)) {
            mesh=GroupedObjMesh.read(reader);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load reservoir model",failure); }
        pose.pushPose(); pose.translate(.5,.5,.5);
        switch(reservoir.facing()) {
            case DOWN->pose.mulPose(Axis.XP.rotationDegrees(-90));
            case UP->pose.mulPose(Axis.XP.rotationDegrees(90));
            case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST->pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));
            default->{}
        }
        pose.translate(0,0,-.5); var frame=pose.last(); var out=TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(TEXTURE));
        for (var group:mesh.groups().values()) for (var vertex:group)
            out.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(255,255,255,255).uv(vertex.u(),vertex.v())
                    .overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
        pose.popPose();
        if (reservoir.getLevel()!=null && reservoir.amount()>0) liquid(reservoir,partial,pose,buffers);
    }
    private static void liquid(EssentiaReservoirBlockEntity reservoir,float partial,PoseStack pose,MultiBufferSource buffers) {
        var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.parse("thaumcraft:block/animatedglow"));
        var out=TCRenderTypes.buffer(buffers,TCRenderTypes.ESSENTIA_LIQUID);
        float red=reservoir.red(),green=reservoir.green(),blue=reservoir.blue();
        // Virtual block entities do not receive the world's client ticker.
        if (reservoir.displayAspect()==null) {
            var aspects=reservoir.revealedAspects().aspects();
            if (aspects.isEmpty())return;
            var level=Minecraft.getInstance().level;
            double time=((level==null?0:level.getGameTime())+partial)/20d;
            int index=Math.floorMod((int)Math.floor(time),aspects.size());
            int from=aspects.get(index).color(),to=aspects.get((index+1)%aspects.size()).color();
            float blend=(float)(time-Math.floor(time));
            red=net.minecraft.util.Mth.lerp(blend,from>>16&255,to>>16&255)/255f;
            green=net.minecraft.util.Mth.lerp(blend,from>>8&255,to>>8&255)/255f;
            blue=net.minecraft.util.Mth.lerp(blend,from&255,to&255)/255f;
        }
        float low=3/16f,high=low+10/16f*reservoir.amount()/256f;
        float[][] vertices={{low,low,low},{13/16f,low,low},{13/16f,low,13/16f},{low,low,13/16f},
                {low,high,low},{13/16f,high,low},{13/16f,high,13/16f},{low,high,13/16f}};
        int[][] faces={{0,1,2,3},{7,6,5,4},{1,0,4,5},{3,2,6,7},{0,3,7,4},{2,1,5,6}};
        for (int face=0;face<6;face++) for (int index:faces[face]) {
            var vertex=vertices[index]; float u=face<4 ? vertex[0] : vertex[2],v=face<2 ? vertex[2] : 1-vertex[1];
            out.vertex(pose.last().pose(),vertex[0],vertex[1],vertex[2]).color(red,green,blue,.9f)
                    .uv(sprite.getU(u*16),sprite.getV(v*16)).uv2(200).endVertex();
        }
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            private final EssentiaReservoirRenderer renderer=new EssentiaReservoirRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                var state=TCArcane.ESSENTIA_RESERVOIR.get().defaultBlockState();
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
                renderer.render(new EssentiaReservoirBlockEntity(net.minecraft.core.BlockPos.ZERO,state),0,pose,buffers,light,overlay);
            }
        };
        return itemRenderer;
    }
}
