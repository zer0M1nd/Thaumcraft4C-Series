package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public class CorpseEffigyItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final net.minecraft.client.model.geom.ModelPart model=CorpseEffigyModel.layer().bakeRoot();
    public CorpseEffigyItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.725,.5);pose.scale(.45f,-.45f,-.45f);
        model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumichorizons:textures/models/corpseeffigy.png"))),light,overlay);
        pose.popPose();
    }
}
