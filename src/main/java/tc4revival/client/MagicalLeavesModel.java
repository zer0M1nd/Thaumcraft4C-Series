package tc4revival.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/** Preserve the old opaque fast-leaves texture and hide shared magical-leaf faces in fast mode. */
public  class MagicalLeavesModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> NEIGHBORS=new ModelProperty<>();
    private final BakedModel fast;
    public MagicalLeavesModel(BakedModel normal,BakedModel fast) { super(normal); this.fast=fast; }
    private static boolean fast() { return Minecraft.getInstance().options.graphicsMode().get()==GraphicsStatus.FAST; }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random) { return (fast() ? fast : originalModel).getQuads(state,side,random); }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType layer) {
        if (!fast()) return originalModel.getQuads(state,side,random,data,layer);
        Integer neighbors=data.get(NEIGHBORS);
        if (side!=null && neighbors!=null && (neighbors & 1<<side.ordinal())!=0) return List.of();
        return fast.getQuads(state,side,random,data,layer);
    }
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData previous) {
        int neighbors=0;
        for (var side:Direction.values()) if (level.getBlockState(pos.relative(side)).getBlock() instanceof tc4revival.content.MagicalLeavesBlock) neighbors|=1<<side.ordinal();
        return previous.derive().with(NEIGHBORS,neighbors).build();
    }
}
