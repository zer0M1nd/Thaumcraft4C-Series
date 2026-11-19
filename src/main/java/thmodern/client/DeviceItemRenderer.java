package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import thmodern.*;
import thmodern.node.*;

public final class DeviceItemRenderer extends BlockEntityWithoutLevelRenderer{
    private final java.util.Map<net.minecraft.world.level.block.Block,BlockEntity> preview=new java.util.HashMap<>();
    private final NodeDeviceRenderer<BlockEntity> renderer=new NodeDeviceRenderer<>(null);
    private final RecombinatorRenderer recombinator=new RecombinatorRenderer(null);
    public DeviceItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!(stack.getItem() instanceof BlockItem item))return;
        var block=item.getBlock();var entity=preview.computeIfAbsent(block,b->((net.minecraft.world.level.block.EntityBlock)b).newBlockEntity(net.minecraft.core.BlockPos.ZERO,b.defaultBlockState()));
        pose.pushPose();
        // GUI transforms already come from each item's display model. Applying a
        // second rotation here tilts the amplifier sideways and turns the base up.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),pose,buffers,light,overlay);
        if(entity instanceof TransductionAmplifierEntity amplifier)new AmplifierRenderer(null).render(amplifier,0,pose,buffers,light,overlay);
        else if(entity instanceof RecombinatorEntity node)recombinator.render(node,0,pose,buffers,light,overlay);
        else renderer.render(entity,0,pose,buffers,light,overlay);pose.popPose();
    }
}
