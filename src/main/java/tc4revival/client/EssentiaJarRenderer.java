package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.EssentiaJarBlockEntity;
import tc4revival.content.TCArcane;
import tc4revival.content.TCItems;

public  class EssentiaJarRenderer implements BlockEntityRenderer<EssentiaJarBlockEntity> {
    private static final ResourceLocation LIQUID=ResourceLocation.parse("thaumcraft:block/animatedglow");
    private static final ResourceLocation LABEL=ResourceLocation.parse("thaumcraft:textures/models/label.png");
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public EssentiaJarRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EssentiaJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) { contents(jar,pose,buffers,light); }
    private static void contents(EssentiaJarBlockEntity jar,PoseStack pose,MultiBufferSource buffers,int light) {
        if (jar.aspect()!=null && jar.amount()>0) liquid(jar,pose,buffers,Math.max(200,light));
        if (jar.filter()==null) return;
        pose.pushPose(); pose.translate(.5,.01,.5); pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(switch(jar.facing()) { case SOUTH->180; case EAST->90; case WEST->270; default->0; }));
        pose.translate(0,-.4,.315);
        if (tc4revival.config.TCConfig.CLIENT.crookedLabels.get()) pose.mulPose(Axis.ZP.rotationDegrees((jar.filter().tag().hashCode()+jar.getBlockPos().getX()+jar.facing().get3DDataValue())%4-2));
        // Label ink uses the selected aspect color, independently of discovery state.
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.worldTranslucent(LABEL)),pose.last().pose(),.25f,1,1,0,0,1,1,1,1,light);
        pose.translate(0,0,.001); pose.mulPose(Axis.YP.rotationDegrees(180));
        var icon=thaumcraft.api.client.ClientAspectViews.view(jar.filter()).texture(); int color=thaumcraft.api.client.ClientAspectViews.view(jar.filter()).color();
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.worldTranslucent(icon)),pose.last().pose(),.168f,1,1,0,0,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.8f,light);
        pose.popPose();
    }
    private static void liquid(EssentiaJarBlockEntity jar,PoseStack pose,MultiBufferSource buffers,int light) {
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LIQUID);
        var out=TCRenderTypes.buffer(buffers,TCRenderTypes.ESSENTIA_LIQUID); int color=thaumcraft.api.client.ClientAspectViews.view(jar.aspect()).color();
        float low=.0725f,high=low+jar.amount()/64f*.625f;
        float[][] vertices={{.25f,low,.25f},{.75f,low,.25f},{.75f,low,.75f},{.25f,low,.75f},
                {.25f,high,.25f},{.75f,high,.25f},{.75f,high,.75f},{.25f,high,.75f}};
        int[][] faces={{0,1,2,3},{7,6,5,4},{1,0,4,5},{3,2,6,7},{0,3,7,4},{2,1,5,6}};
        for (int face=0;face<faces.length;face++) for (int corner:faces[face]) {
            var point=vertices[corner]; float u=face<4 ? point[0] : point[2],v=face<2 ? point[2] : 1-(point[1]-.01f);
            out.vertex(pose.last().pose(),point[0],point[1],point[2]).color(color>>16&255,color>>8&255,color&255,255)
                    .uv(sprite.getU(u*16),sprite.getV(v*16)).uv2(light).endVertex();
        }
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                boolean voidJar=stack.is(TCItems.VOID_JAR.get()) || stack.is(TCItems.FILLED_VOID_JAR.get());
                var state=(voidJar ? TCArcane.VOID_JAR.get() : TCArcane.ESSENTIA_JAR.get()).defaultBlockState();
                var jar=new EssentiaJarBlockEntity(net.minecraft.core.BlockPos.ZERO,state); jar.readItem(stack);
                contents(jar,pose,buffers,light); Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
            }
        };
        return itemRenderer;
    }
}
