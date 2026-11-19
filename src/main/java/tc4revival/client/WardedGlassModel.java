package tc4revival.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/** Keep the cube's native face UVs and replace only the selected connected texture. */
public  class WardedGlassModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<int[]> MASKS=new ModelProperty<>();
    public WardedGlassModel(BakedModel original) { super(original); }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean useAmbientOcclusion(BlockState state) { return false; }
    @Override public boolean useAmbientOcclusion(BlockState state,RenderType layer) { return false; }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data) { return ChunkRenderTypeSet.of(RenderType.translucent()); }
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData previous) {
        int[] masks=new int[6];
        for (var face:Direction.values()) {
            var right=switch(face) { case NORTH->Direction.WEST; case SOUTH,UP,DOWN->Direction.EAST; case WEST->Direction.SOUTH; case EAST->Direction.NORTH; };
            // Vanilla's bottom-face V axis runs north, opposite to the top face.
            // Mirroring the neighbour mask leaves borders on internal seams below.
            var down=switch(face) { case UP->Direction.SOUTH; case DOWN->Direction.NORTH; default->Direction.DOWN; };
            int bit=0,mask=0;
            for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++) {
                if (x==0 && y==0) continue;
                if (level.getBlockState(pos.relative(right,x).relative(down,y)).is(state.getBlock())) mask|=1<<bit;
                bit++;
            }
            masks[face.ordinal()]=mask;
        }
        return previous.derive().with(MASKS,masks).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType layer) {
        if (layer!=null && layer!=RenderType.translucent()) return List.of();
        var source=originalModel.getQuads(state,side,random);
        var masks=data.get(MASKS); if (masks==null) return source;
        var result=new ArrayList<BakedQuad>(source.size());
        for (var quad:source) {
            int index=ConnectedFaces.texture(masks[quad.getDirection().ordinal()])+1;
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("thaumcraft","block/warded_glass_"+index));
            var old=quad.getSprite(); int[] vertices=quad.getVertices().clone(); int stride=vertices.length/4;
            for (int vertex=0;vertex<4;vertex++) {
                int offset=vertex*stride;
                float u=(Float.intBitsToFloat(vertices[offset+4])-old.getU0())/(old.getU1()-old.getU0());
                float v=(Float.intBitsToFloat(vertices[offset+5])-old.getV0())/(old.getV1()-old.getV0());
                vertices[offset+4]=Float.floatToRawIntBits(sprite.getU(u*16)); vertices[offset+5]=Float.floatToRawIntBits(sprite.getV(v*16));
            }
            result.add(new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),sprite,false));
        }
        return result;
    }
}
