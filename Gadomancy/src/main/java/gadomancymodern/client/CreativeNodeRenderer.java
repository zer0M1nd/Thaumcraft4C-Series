package gadomancymodern.client;
import net.minecraft.client.renderer.*;
import net.minecraft.client.Minecraft;
/** Original four 50-aspect layers on three perpendicular item planes; no jar geometry. */
public class CreativeNodeRenderer extends BlockEntityWithoutLevelRenderer {
    protected static CreativeNodeRenderer renderer;
    public CreativeNodeRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static CreativeNodeRenderer instance(){if(renderer==null)renderer=new CreativeNodeRenderer();return renderer;}
    @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext display,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){var aspects=new tc4revival.aspect.AspectAmounts();for(String aspect:java.util.List.of("aer","ignis","terra","aqua"))aspects.add(tc4revival.aspect.TCAspects.catalog().require(aspect),50);var data=new tc4revival.content.NodeJarData(aspects,gadomancymodern.item.CreativeNodeItem.type(stack),null,"");pose.pushPose();pose.translate(.5,.5,.5);thaumcraft.api.client.DeviceRendering.nodeItem(data,pose,buffers);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));thaumcraft.api.client.DeviceRendering.nodeItem(data,pose,buffers);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));thaumcraft.api.client.DeviceRendering.nodeItem(data,pose,buffers);pose.popPose();}
}
