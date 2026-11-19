package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import tc4revival.content.LevitatorBlockEntity;

public  class LevitatorRenderer implements BlockEntityRenderer<LevitatorBlockEntity> {
    public LevitatorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(LevitatorBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (entity.getLevel()==null) return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("thaumcraft:block/animatedglow"));
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)); var frame=pose.last();
        int brightness=entity.powered()?light:180;
        for (Direction side:Direction.values()) {
            if (side==Direction.DOWN) continue;
            var neighbor=entity.getBlockPos().relative(side);
            if (entity.getLevel().getBlockState(neighbor).isSolidRender(entity.getLevel(),neighbor)) continue;
            float[][] corners=switch(side) {
                case UP->new float[][]{{0,.99f,0},{0,.99f,1},{1,.99f,1},{1,.99f,0}};
                case NORTH->new float[][]{{1,0,.01f},{0,0,.01f},{0,1,.01f},{1,1,.01f}};
                case SOUTH->new float[][]{{0,0,.99f},{1,0,.99f},{1,1,.99f},{0,1,.99f}};
                case EAST->new float[][]{{.99f,0,1},{.99f,0,0},{.99f,1,0},{.99f,1,1}};
                default->new float[][]{{.01f,0,0},{.01f,0,1},{.01f,1,1},{.01f,1,0}};
            };
            int tint=side==Direction.UP?40960:0xdd11ff;
            for (int i=0;i<4;i++) out.vertex(frame.pose(),corners[i][0],corners[i][1],corners[i][2]).color(tint>>16&255,tint>>8&255,tint&255,255)
                    .uv(i==0 || i==3?sprite.getU0():sprite.getU1(),i<2?sprite.getV1():sprite.getV0()).overlayCoords(overlay).uv2(brightness)
                    .normal(frame.normal(),side.getStepX(),side.getStepY(),side.getStepZ()).endVertex();
        }
    }
}
