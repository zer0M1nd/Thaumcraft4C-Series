package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import net.minecraftforge.client.model.BakedModelWrapper;
import tc4revival.content.JarLabelItem;

/** Preserve the resource-pack item model and add a coloured aspect on both faces. */
public class JarLabelRenderer extends BlockEntityWithoutLevelRenderer {
    private static BakedModel base;
    private static JarLabelRenderer instance;
    private JarLabelRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static JarLabelRenderer instance(){if(instance==null)instance=new JarLabelRenderer();return instance;}
    public static BakedModel wrap(BakedModel model){base=model;return new LabelModel(model);}
    private static class LabelModel extends BakedModelWrapper<BakedModel> {
        LabelModel(BakedModel model){super(model);}
        @Override public boolean isCustomRenderer(){return true;}
        @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left){originalModel.applyTransform(context,pose,left);return this;}
        @Override public java.util.List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous){return java.util.List.of(this);}
        @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides(){
            return new net.minecraft.client.renderer.block.model.ItemOverrides(){
                @Override public BakedModel resolve(BakedModel model,ItemStack stack,net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.entity.LivingEntity entity,int seed){
                    var selected=originalModel.getOverrides().resolve(originalModel,stack,level,entity,seed);
                    return selected==null||selected==originalModel?LabelModel.this:new LabelModel(selected);
                }
            };
        }
        BakedModel baseModel(){return originalModel;}
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(base==null)return;
        var minecraft=Minecraft.getInstance();
        var resolved=minecraft.getItemRenderer().getModel(stack,minecraft.level,null,0);
        var paper=resolved instanceof LabelModel model?model.baseModel():base;
        pose.pushPose();pose.translate(.5,.5,.5);
        minecraft.getItemRenderer().render(stack,ItemDisplayContext.NONE,false,pose,buffers,light,overlay,paper);
        pose.popPose();
        // Item and entity translucent layers otherwise flush in the opposite order:
        // the decal writes depth first and punches a hole in the paper behind it.
        if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch();
        if(!(stack.getItem() instanceof JarLabelItem label))return;
        var aspects=label.storedAspects(stack);if(aspects.isEmpty())return;
        var aspect=aspects.view().keySet().iterator().next();int c=thaumcraft.api.client.ClientAspectViews.view(aspect).color();
        var out=buffers.getBuffer(RenderType.entityTranslucent(thaumcraft.api.client.ClientAspectViews.view(aspect).texture()));
        for(int face:new int[]{-1,1}) {
            float z=.5f+face*.034f;
            vertex(out,pose,.34375f,.28125f,z,face<0?1:0,1,c,light,overlay,face);
            vertex(out,pose,.71875f,.28125f,z,face<0?0:1,1,c,light,overlay,face);
            vertex(out,pose,.71875f,.65625f,z,face<0?0:1,0,c,light,overlay,face);
            vertex(out,pose,.34375f,.65625f,z,face<0?1:0,0,c,light,overlay,face);
        }
        if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch(RenderType.entityTranslucent(thaumcraft.api.client.ClientAspectViews.view(aspect).texture()));
    }
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int c,int light,int overlay,int normal){
        out.vertex(pose.last().pose(),x,y,z).color(c>>16&255,c>>8&255,c&255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),0,0,normal).endVertex();
    }
}
