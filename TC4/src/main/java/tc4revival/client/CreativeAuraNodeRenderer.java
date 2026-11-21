package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Legacy creative node: four forty-vis aspects, drawn on three intersecting planes. */
public  class CreativeAuraNodeRenderer extends BlockEntityWithoutLevelRenderer {
    private static CreativeAuraNodeRenderer instance;
    private CreativeAuraNodeRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static CreativeAuraNodeRenderer instance() {
        if (instance==null) instance=new CreativeAuraNodeRenderer();
        return instance;
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var aspects=new AspectAmounts();
        for (String tag:new String[]{"aer","ignis","terra","aqua"}) aspects.add(TCAspects.catalog().require(tag),40);
        var data=new NodeJarData(aspects,AuraNodeType.NORMAL,null,"");
        pose.pushPose(); pose.translate(.5,.5,.5); pose.scale(2,2,2);
        NodeJarRenderer.itemNode(data,pose,buffers);
        pose.mulPose(Axis.YP.rotationDegrees(90)); NodeJarRenderer.itemNode(data,pose,buffers);
        pose.mulPose(Axis.XP.rotationDegrees(90)); NodeJarRenderer.itemNode(data,pose,buffers);
        pose.popPose();
    }
}
