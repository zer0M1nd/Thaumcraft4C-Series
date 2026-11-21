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
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/** Keeps the old coordinate/face texture selection without adding persistent block states. */
public  class EldritchStoneModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<BlockPos> POSITION=new ModelProperty<>();
    private final boolean rock,stairs;
    public EldritchStoneModel(BakedModel original,boolean rock,boolean stairs) { super(original); this.rock=rock; this.stairs=stairs; }
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData previous) {
        return originalModel.getModelData(level,pos,state,previous).derive().with(POSITION,pos.immutable()).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType layer) {
        var source=originalModel.getQuads(state,side,random,data,layer); BlockPos pos=data.get(POSITION);
        if (pos==null) return source;
        var result=new ArrayList<BakedQuad>(source.size());
        for (BakedQuad quad:source) {
            Direction face=quad.getDirection(); int variant;
            if (rock) {
                int horizontal=face.getAxis()==Direction.Axis.X ? pos.getZ() : pos.getX();
                int vertical=face.getAxis()==Direction.Axis.Y ? pos.getZ() : pos.getY();
                variant=Math.abs(horizontal%2)+Math.abs(vertical%2)*2;
            } else {
                String coordinates=""+pos.getX()+pos.getY()+pos.getZ();
                // Preserve int overflow, concatenation without separators and one draw per face.
                long seed=Math.abs(coordinates.hashCode()*100)+1;
                variant=new java.util.Random(seed).nextInt(12345+face.get3DDataValue()+(stairs ? 100 : 0))%4;
            }
            int base=state!=null && state.is(tc4revival.content.TCEldritch.TRAP.get()) ? 5 : 1;
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.parse("thaumcraft:block/"+(rock ? "er_" : "es_")+(variant+base)));
            var old=quad.getSprite(); int[] vertices=quad.getVertices().clone(); int stride=vertices.length/4;
            for (int vertex=0;vertex<4;vertex++) {
                int uv=vertex*stride+4;
                float u=(Float.intBitsToFloat(vertices[uv])-old.getU0())/(old.getU1()-old.getU0());
                float v=(Float.intBitsToFloat(vertices[uv+1])-old.getV0())/(old.getV1()-old.getV0());
                vertices[uv]=Float.floatToRawIntBits(sprite.getU0()+u*(sprite.getU1()-sprite.getU0()));
                vertices[uv+1]=Float.floatToRawIntBits(sprite.getV0()+v*(sprite.getV1()-sprite.getV0()));
            }
            result.add(new BakedQuad(vertices,quad.getTintIndex(),face,sprite,quad.isShade(),quad.hasAmbientOcclusion()));
        }
        return result;
    }
}
