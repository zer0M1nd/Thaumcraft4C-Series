package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.TableBlock;
import tc4revival.content.TableItem;
import tc4revival.content.TCItems;

public  class TableItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static TableItemRenderer instance;
    private final ResearchTableRenderer renderer = new ResearchTableRenderer(null);
    private TableItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    public static TableItemRenderer instance() { if (instance == null) instance = new TableItemRenderer(); return instance; }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean research = stack.getItem() instanceof TableItem item && item.research();
        pose.pushPose(); pose.translate(0.5, 0.5, 0.5); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(-0.5, -0.5, -0.5);
        if (research) pose.translate(-0.5, 0, 0);
        renderer.renderModel(research ? TableBlock.Part.MASTER : TableBlock.Part.SINGLE, research ? Direction.EAST : Direction.SOUTH, research,
                research ? new ItemStack(TCItems.RESEARCH_NOTES.get()) : ItemStack.EMPTY, pose, buffers, light, overlay);
        pose.popPose();
    }
}
