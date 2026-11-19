package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.NodeJarBlockEntity;
import tc4revival.content.NodeJarData;

public  class NodeJarRenderer implements BlockEntityRenderer<NodeJarBlockEntity> {
    private static final ResourceLocation NODE=ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public NodeJarRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(NodeJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        thaumcraft.api.client.DeviceRendering.render(new thaumcraft.api.client.DeviceRendering.Context<>(jar,partial,pose,buffers,light,overlay),()->renderDefault(jar,partial,pose,buffers,light,overlay));
    }
    private void renderDefault(NodeJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var data=jar.contents(); pose.pushPose(); pose.translate(.15,.05,.15); pose.scale(.7f,.7f,.7f);
        EnergizedNodeRenderer.renderNode(jar.getBlockPos(),data.aspects(),data.type(),data.modifier(),true,false,64,pose,buffers); pose.popPose();
        // TC4 binds ModelJar's texture and scales its pose, but never draws that model.
        // The actual glass is the two cuboids in the block model, without that animation.
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) {
            var mc=Minecraft.getInstance();
            itemRenderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()) {
                @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                    var data=NodeJarData.read(stack.hasTag() ? stack.getTag() : new net.minecraft.nbt.CompoundTag());
                    pose.pushPose(); pose.translate(.5,.4,.5);
                    itemNode(data,pose,buffers); pose.mulPose(Axis.YP.rotationDegrees(90)); itemNode(data,pose,buffers);
                    pose.mulPose(Axis.XP.rotationDegrees(90)); itemNode(data,pose,buffers); pose.popPose();
                    mc.getBlockRenderer().renderSingleBlock(tc4revival.content.TCArcane.NODE_JAR.get().defaultBlockState(),pose,buffers,light,overlay);
                }
            };
        }
        return itemRenderer;
    }
    public static void itemNode(NodeJarData data,PoseStack pose,MultiBufferSource buffers) {
        var aspects=thaumcraft.api.client.ClientAspectViews.project(data.aspects(),thaumcraft.api.aspects.AspectViews.Purpose.NODE); if (aspects.isEmpty()) return;
        var viewer=Minecraft.getInstance().getCameraEntity(); int ticks=viewer==null ? 0 : viewer.tickCount;
        float alpha=.5f;
        if (data.modifier()!=null) alpha*=switch(data.modifier()) { case BRIGHT->1.5f; case PALE->.66f; case FADING->(float)Math.sin(ticks/3f)*.25f+.33f; };
        int frame=(int)((System.nanoTime()/40_000_000+1)%32),index=0; double sum=0;
        for (var aspect:aspects.aspects()) {
            int amount=NodeVisualSize.amount(aspects.amount(aspect)); sum+=amount; int divisor=14-index++;
            float size=.2f+((float)Math.sin(ticks/(float)(divisor==0 ? 1 : divisor))*.25f+.5f)*amount/50f;
            strip(pose,buffers,size,alpha/aspects.size()*(aspect.blendFactor()==771 ? 1.5f : 1),0,frame,aspect.color(),aspect.blendFactor()==771);
        }
        int row=switch(data.type()) { case UNSTABLE->6; case DARK->2; case TAINTED->5; case PURE->4; case HUNGRY->3; default->1; };
        float size=(float)(.1+sum/aspects.size()/150)*(data.type()==tc4revival.content.AuraNodeType.HUNGRY ? .75f : 1);
        strip(pose,buffers,size,alpha,row,frame,0xffffff,data.type()==tc4revival.content.AuraNodeType.DARK || data.type()==tc4revival.content.AuraNodeType.TAINTED);
    }
    private static void strip(PoseStack pose,MultiBufferSource buffers,float size,float alpha,int row,int frame,int color,boolean dark) {
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,dark ? TCRenderTypes.softTranslucent(NODE) : TCRenderTypes.softParticles(NODE)),pose.last().pose(),size/2,
                (frame+1)/32f,(row+1)/32f,frame/32f,row/32f,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha,220);
    }
}
