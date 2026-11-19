package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import tc4revival.content.EldritchVoidBlockEntity;

public  class EldritchVoidRenderer implements BlockEntityRenderer<EldritchVoidBlockEntity> {
    public EldritchVoidRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EldritchVoidBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var level=entity.getLevel(); if (level==null) return;
        if (!entity.getBlockState().getValue(tc4revival.content.EldritchVoidBlock.EXPOSED)) return;
        var pos=entity.getBlockPos(); var viewer=Minecraft.getInstance().getCameraEntity();
        boolean near=viewer!=null && viewer.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<512;
        for (Direction side:Direction.values()) {
            var neighbor=pos.relative(side);
            if (level.getBlockState(neighbor).isSolidRender(level,neighbor)) continue;
            // Legacy method names describe the inward normal; exposed faces point outward.
            Direction plane=side.getAxis()==Direction.Axis.Y ? side : side.getOpposite();
            float offset=side.getAxisDirection()==Direction.AxisDirection.POSITIVE ? 1 : 0;
            EldritchPlanes.draw(pos,plane,offset,0,1,0,1,near,partial,pose,buffers);
        }
    }
}
