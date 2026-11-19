package tc4revival.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import tc4revival.content.FiniteFluidBlock;

/** Chunk-baked finite surfaces; no block entities or per-frame world scans are needed. */
public  class FiniteFluxModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<List<BakedQuad>> SURFACE = new ModelProperty<>();
    public FiniteFluxModel(BakedModel original) { super(original); }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean useAmbientOcclusion(BlockState state) { return false; }
    @Override public boolean useAmbientOcclusion(BlockState state, RenderType layer) { return false; }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) { return ChunkRenderTypeSet.of(RenderType.translucent()); }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
        var quads = data.get(SURFACE);
        return side == null && (layer == null || layer == RenderType.translucent()) && quads != null ? quads : List.of();
    }
    @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData previous) {
        if (!(state.getBlock() instanceof FiniteFluidBlock fluid)) return previous;
        var quads = new ArrayList<BakedQuad>();
        var sprite = getParticleIcon();
        boolean gas = state.getFluidState().getFluidType().isLighterThanAir();
        // Unconfined gas renders as a full cube in TC4; the hanging surface appears under a solid ceiling.
        boolean cube = gas && !level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
        double[] heights = cube ? new double[]{1, 1, 1, 1} : corners(level, pos, fluid, gas);
        double[] y = new double[4];
        for (int i = 0; i < 4; i++) y[i] = gas ? 1 - heights[i] : heights[i];
        Direction freeFace = gas ? Direction.DOWN : Direction.UP, fixedFace = freeFace.getOpposite();
        if (visible(level, pos, fluid, freeFace)) {
            double[] uv = surfaceUV(fluid.flowVector(level, pos), cube);
            double offset = cube ? 0 : gas ? .001 : -.001;
            double[][] vertices = {{0,y[0]+offset,0,uv[0],uv[1]}, {0,y[1]+offset,1,uv[2],uv[3]},
                    {1,y[2]+offset,1,uv[4],uv[5]}, {1,y[3]+offset,0,uv[6],uv[7]}};
            quad(quads, sprite, freeFace, vertices, gas, cube && gas ? .5f : 1, light(level, pos));
        }
        if (visible(level, pos, fluid, fixedFace)) {
            double fixedY = gas ? 1 : 0;
            double[][] vertices = {{0,fixedY,0,0,0}, {0,fixedY,1,0,16}, {1,fixedY,1,16,16}, {1,fixedY,0,16,0}};
            quad(quads, sprite, fixedFace, vertices, !gas, gas ? 1 : .5f, light(level, pos.below()));
        }
        var sides = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        int[][] endpoints = {{0,3},{2,1},{1,0},{3,2}};
        double[][] xz = {{0,0},{0,1},{1,1},{1,0}};
        for (int side = 0; side < 4; side++) {
            Direction direction = sides[side];
            if (!visible(level, pos, fluid, direction)) continue;
            int a = endpoints[side][0], b = endpoints[side][1];
            double shiftX = cube ? 0 : -direction.getStepX() * .001, shiftZ = cube ? 0 : -direction.getStepZ() * .001;
            double xa = xz[a][0]+shiftX, za = xz[a][1]+shiftZ, xb = xz[b][0]+shiftX, zb = xz[b][1]+shiftZ;
            double farV = cube ? 16 : 8, va = cube ? 0 : (1-heights[a])*8, vb = cube ? 0 : (1-heights[b])*8;
            double fixedY = gas ? 1 : 0;
            double[][] vertices = {{xa,y[a],za,0,va}, {xb,y[b],zb,farV,vb}, {xb,fixedY,zb,farV,farV}, {xa,fixedY,za,0,farV}};
            quad(quads, sprite, direction, vertices, gas, side < 2 ? .8f : .6f, light(level, pos.relative(direction)));
        }
        return previous.derive().with(SURFACE, List.copyOf(quads)).build();
    }
    private static boolean visible(BlockAndTintGetter level, BlockPos pos, FiniteFluidBlock fluid, Direction face) {
        var neighbor = pos.relative(face); var state = level.getBlockState(neighbor);
        return !state.is(fluid) && !state.isSolidRender(level, neighbor);
    }
    private static double sample(BlockAndTintGetter level, BlockPos pos, FiniteFluidBlock fluid, boolean gas) {
        var state = level.getBlockState(pos); var opposite = level.getBlockState(gas ? pos.below() : pos.above());
        if (state.is(fluid)) {
            // MaterialTaint itself was not a liquid, so same-flux neighbors do not enter this branch.
            if (!opposite.getFluidState().isEmpty() && (!(opposite.getBlock() instanceof FiniteFluidBlock other) || other.liquidMaterial())) return 1;
            if (state.getValue(FiniteFluidBlock.QUANTA) == fluid.capacity()) return .875;
        }
        if (!FiniteFluidBlock.solidMaterial(state) && opposite.is(fluid)) return 1;
        return fluid.quantity(level, pos) / (double)fluid.capacity() * .875;
    }
    private static double[] corners(BlockAndTintGetter level, BlockPos pos, FiniteFluidBlock fluid, boolean gas) {
        double center = sample(level, pos, fluid, gas);
        if (center == 1) return new double[]{1,1,1,1};
        double[] result = new double[4];
        int[][] offsets = {{-1,-1},{-1,1},{1,1},{1,-1}};
        for (int i = 0; i < 4; i++) {
            int x = offsets[i][0], z = offsets[i][1];
            double[] samples = {sample(level,pos.offset(x,0,z),fluid,gas), sample(level,pos.offset(x,0,0),fluid,gas), sample(level,pos.offset(0,0,z),fluid,gas),center};
            double preferred = 0, sum = 0; int count = 0;
            for (double height : samples) {
                if (height >= .875 && preferred != 1) preferred = height;
                if (height >= 0) { sum += height; count++; }
            }
            result[i] = preferred > 0 ? preferred : count == 0 ? 0 : sum/count;
        }
        return result;
    }
    private static double[] surfaceUV(net.minecraft.world.phys.Vec3 flow, boolean cube) {
        if (cube || flow.x == 0 && flow.z == 0) return new double[]{0,0,0,16,16,16,16,0};
        double angle = Math.atan2(flow.z, flow.x) - Math.PI/2, sine = Math.sin(angle)*4, cosine = Math.cos(angle)*4;
        return new double[]{8-cosine-sine,8-cosine+sine,8-cosine+sine,8+cosine+sine,
                8+cosine+sine,8+cosine-sine,8+cosine-sine,8-cosine-sine};
    }
    private static int light(BlockAndTintGetter level, BlockPos pos) {
        int here = LevelRenderer.getLightColor(level, pos), above = LevelRenderer.getLightColor(level, pos.above());
        return Math.max(here & 0xffff, above & 0xffff) | Math.max(here >>> 16, above >>> 16) << 16;
    }
    private static void quad(List<BakedQuad> target, TextureAtlasSprite sprite, Direction face, double[][] positions, boolean reverse, float shade, int light) {
        int[] packed = new int[32]; int channel = Math.round(shade * 255), color = 0xff000000 | channel << 16 | channel << 8 | channel;
        int normal = (face.getStepX()*127 & 255) | (face.getStepY()*127 & 255) << 8 | (face.getStepZ()*127 & 255) << 16;
        for (int i = 0; i < 4; i++) {
            var vertex = positions[reverse ? 3-i : i]; int start = i*8;
            packed[start] = Float.floatToRawIntBits((float)vertex[0]); packed[start+1] = Float.floatToRawIntBits((float)vertex[1]); packed[start+2] = Float.floatToRawIntBits((float)vertex[2]);
            packed[start+3] = color; packed[start+4] = Float.floatToRawIntBits(sprite.getU(vertex[3])); packed[start+5] = Float.floatToRawIntBits(sprite.getV(vertex[4]));
            packed[start+6] = light; packed[start+7] = normal;
        }
        target.add(new BakedQuad(packed, -1, face, sprite, false));
    }
}
