package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import tc4revival.content.WandFocusItem;
import tc4revival.content.WardedBlockEntity;
import tc4revival.content.WardingFocusItem;

public  class WardedBlockRenderer implements BlockEntityRenderer<WardedBlockEntity> {
    public WardedBlockRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(WardedBlockEntity ward, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = ward.getLevel(); if (level == null) return;
        var mc = Minecraft.getInstance(); var original = ward.original(); var pos = ward.getBlockPos();
        var dispatcher = mc.getBlockRenderer(); var model = dispatcher.getBlockModel(original); var random = RandomSource.create(original.getSeed(pos));
        for (var type : model.getRenderTypes(original, random, ModelData.EMPTY)) {
            dispatcher.getModelRenderer().tesselateBlock(level, model, original, pos, pose, buffers.getBuffer(type), false, random, original.getSeed(pos), overlay, ModelData.EMPTY, type);
        }
        var player = mc.player;
        if (player == null || !(WandFocusItem.attached(player.getMainHandItem()).getItem() instanceof WardingFocusItem)) return;
        boolean owner = ward.ownedBy(player); float ticks = player.tickCount;
        ConnectedFaces.render(pos, neighbour -> level.getBlockEntity(neighbour) instanceof WardedBlockEntity other && Objects.equals(other.owner(), ward.owner()),
                neighbour -> level.getBlockEntity(neighbour) instanceof WardedBlockEntity other && other.original().equals(original), pose, buffers,
                (float) Math.sin(ticks / 2 + pos.getX()) * .2f + .8f,
                (float) Math.sin(ticks / 3 + pos.getY()) * .2f + (owner ? .7f : .28f),
                (float) Math.sin(ticks / 4 + pos.getZ()) * .2f + .28f, owner ? .5f : .25f, false);
    }
}
