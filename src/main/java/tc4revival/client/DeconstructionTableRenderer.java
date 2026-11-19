package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.content.*;

public  class DeconstructionTableRenderer implements BlockEntityRenderer<DeconstructionTableBlockEntity> {
    private static final ResourceLocation TABLE=ResourceLocation.parse("thaumcraft:textures/models/decontable.png");
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public DeconstructionTableRenderer(BlockEntityRendererProvider.Context context) {}
    private static void table(PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        ArcaneWorkbenchRenderer.renderTable(pose,buffers,light,overlay,TABLE);
        // The old EntityItem renderer added a 0.1-block resting lift inside the 0.8 scale.
        // Without it the scanner's entire thickness sits below the table top (y=1).
        pose.pushPose(); pose.translate(.5,1.0,.5); pose.scale(.8f,.8f,.8f);
        // Mounted scanner uses the legacy frame's 5/16 scale without animated bobbing.
        pose.mulPose(Axis.YP.rotationDegrees(-90)); pose.scale(.3125f,.3125f,.3125f);
        ThaumometerRenderer.instance().renderModel(pose,buffers,light,overlay,false);
        pose.popPose();
    }
    @Override public void render(DeconstructionTableBlockEntity table,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        table(pose,buffers,light,overlay);
        var mc=Minecraft.getInstance(); float ticks=mc.getCameraEntity()==null ? 0 : mc.getCameraEntity().tickCount+partial;
        if (!table.isEmpty()) {
            pose.pushPose(); pose.translate(.5,1.15+Math.sin(Math.sin(ticks/14)*.2+.2)*.1+.1,.5); pose.mulPose(Axis.YP.rotationDegrees(ticks%360));
            if (table.getItem(0).getItem() instanceof BlockItem) pose.scale(2,2,2);
            MultiBufferSource ghost=type->new AlphaVertex(TCRenderTypes.buffer(buffers,TCRenderTypes.deconstructionItem(type)));
            mc.getItemRenderer().renderStatic(table.getItem(0).copyWithCount(1),ItemDisplayContext.GROUND,light,overlay,pose,ghost,table.getLevel(),0);
            pose.popPose();
        }
        var aspect=table.pending();
        if (aspect!=null) {
            pose.pushPose(); pose.translate(.5,1.081,.5); pose.mulPose(Axis.XP.rotationDegrees(90)); pose.mulPose(Axis.ZP.rotationDegrees(ticks%360));
            int color=thaumcraft.api.client.ClientAspectViews.view(aspect).color();
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.runes(thaumcraft.api.client.ClientAspectViews.view(aspect).texture())),pose.last().pose(),.192f,0,0,1,1,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.8f,light);
            pose.popPose();
        }
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) { table(pose,buffers,light,overlay); }
        };
        return itemRenderer;
    }
    private record AlphaVertex(VertexConsumer delegate) implements VertexConsumer {
        public VertexConsumer vertex(double x,double y,double z) { delegate.vertex(x,y,z); return this; }
        public VertexConsumer color(int r,int g,int b,int a) { delegate.color(r,g,b,a*3/4); return this; }
        public VertexConsumer uv(float u,float v) { delegate.uv(u,v); return this; }
        public VertexConsumer overlayCoords(int u,int v) { delegate.overlayCoords(u,v); return this; }
        public VertexConsumer uv2(int u,int v) { delegate.uv2(u,v); return this; }
        public VertexConsumer normal(float x,float y,float z) { delegate.normal(x,y,z); return this; }
        public void endVertex() { delegate.endVertex(); }
        public void defaultColor(int r,int g,int b,int a) { delegate.defaultColor(r,g,b,a*3/4); }
        public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
