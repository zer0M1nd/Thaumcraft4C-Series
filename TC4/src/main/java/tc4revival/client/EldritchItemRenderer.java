package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import tc4revival.content.*;

public  class EldritchItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static EldritchItemRenderer instance;
    private EldritchItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static EldritchItemRenderer instance() { if (instance==null) instance=new EldritchItemRenderer(); return instance; }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        if (item.getBlock() instanceof EldritchCrystalBlock) EldritchCrystalRenderer.draw(Direction.UP,0,pose,buffers,light,overlay);
        else if(item.getBlock() instanceof EldritchCrabSpawnerBlock) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(item.getBlock().defaultBlockState(),pose,buffers,light,overlay);
            EldritchCrabSpawnerRenderer.draw(Direction.NORTH,pose,buffers,light,overlay);
        }
        else if (item.getBlock() instanceof EldritchMonumentBlock block) {
            pose.pushPose();
            if(block.kind()==EldritchMonumentBlock.Kind.OBELISK || block.kind()==EldritchMonumentBlock.Kind.SEGMENT) pose.translate(0,-2,0);
            EldritchMonumentRenderer.draw(block.kind(),false,0,null,0,pose,buffers,light,overlay);
            pose.popPose();
        }
    }
}
