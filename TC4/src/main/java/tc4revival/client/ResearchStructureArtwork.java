package tc4revival.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tc4revival.research.ResearchStructureDisplay;

/** Standalone baked models and the original node animation; never registers inventory items. */
public  class ResearchStructureArtwork {
    private ResearchStructureArtwork() {}
    public static void draw(GuiGraphics graphics,ResearchStructureDisplay display,int x,int y) {
        var mc=Minecraft.getInstance(); var pose=graphics.pose();
        graphics.flush(); pose.pushPose();
        try {
            pose.translate(x+8,y+8,150); pose.scale(16,-16,16);
            pose.mulPose(Axis.XP.rotationDegrees(30)); pose.mulPose(Axis.YP.rotationDegrees(225));
            if (display==ResearchStructureDisplay.NODE) {
                pose.scale(2,2,2);
                var data=ResearchStructureDisplay.node();
                NodeJarRenderer.itemNode(data,pose,graphics.bufferSource());
                pose.mulPose(Axis.YP.rotationDegrees(90)); NodeJarRenderer.itemNode(data,pose,graphics.bufferSource());
                pose.mulPose(Axis.XP.rotationDegrees(90)); NodeJarRenderer.itemNode(data,pose,graphics.bufferSource());
            } else {
                pose.scale(.625f,.625f,.625f); pose.translate(-.5,-.5,-.5);
                var model=mc.getModelManager().getModel(ResourceLocation.parse("thaumcraft:item/"+display.model()));
                mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                        graphics.bufferSource().getBuffer(RenderType.entityCutout(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)),
                        null,model,1,1,1,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            }
            graphics.bufferSource().endBatch();
        } finally { pose.popPose(); }
    }
}
