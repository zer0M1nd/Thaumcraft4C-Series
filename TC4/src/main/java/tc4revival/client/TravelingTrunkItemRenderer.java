package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** The portable item uses a closed chest, including when its stored upgrade is earth. */
public  class TravelingTrunkItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static TravelingTrunkItemRenderer instance;
    private final net.minecraft.client.model.geom.ModelPart chest=TravelingTrunkModel.create().bakeRoot();
    private TravelingTrunkItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static TravelingTrunkItemRenderer instance() { if (instance==null) instance=new TravelingTrunkItemRenderer(); return instance; }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        // Item rendering now starts at a block corner; recenter before the legacy transform.
        pose.translate(.5,.5,.5); pose.scale(1,-1,-1);
        if (context==ItemDisplayContext.GUI) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
        if (context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.translate(-.25,-.5,-.25); if (!context.firstPerson()) pose.translate(1,0,0);
        }
        pose.translate(-.5,-.5,-.5);
        chest.render(pose,buffers.getBuffer(RenderType.entityCutout(ResourceLocation.parse("thaumcraft:textures/models/trunk.png"))),light,overlay);
        pose.popPose();
    }
}
