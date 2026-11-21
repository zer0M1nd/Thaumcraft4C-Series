package tc4revival.client;

import java.util.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.*;

/** Position-seeded wax drips, matching the old world renderer's integer seed and draw order. */
public  class TallowCandleModel extends net.minecraftforge.client.model.BakedModelWrapper<BakedModel> {
    private static final ModelProperty<List<BakedQuad>> DRIPS=new ModelProperty<>();
    public TallowCandleModel(BakedModel original) { super(original); }
    @Override public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData previous) {
        var random=new java.util.Random(pos.getX()+pos.getY()*pos.getZ()); var quads=new ArrayList<BakedQuad>(); int count=1+random.nextInt(5);
        var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/candle"));
        for (int i=0;i<count;i++) {
            boolean side=random.nextBoolean(); int loc=2+random.nextInt(2),height=1+random.nextInt(3);
            var from=new org.joml.Vector3f(i%2==0 ? 5+loc : side ? 5 : 10,0,i%2==0 ? side ? 5 : 10 : 5+loc);
            var to=new org.joml.Vector3f(i%2==0 ? 6+loc : side ? 6 : 11,height,i%2==0 ? side ? 6 : 11 : 6+loc);
            var faces=new EnumMap<Direction,BlockElementFace>(Direction.class);
            for (var face:Direction.values()) faces.put(face,new BlockElementFace(null,0,"#all",new BlockFaceUV(null,0)));
            var element=new BlockElement(from,to,faces,null,true); var bakery=new FaceBakery();
            for (var face:Direction.values()) quads.add(bakery.bakeQuad(from,to,element.faces.get(face),sprite,face,BlockModelRotation.X0_Y0,null,true,net.minecraft.resources.ResourceLocation.parse("thaumcraft:candle_drips")));
        }
        return previous.derive().with(DRIPS,List.copyOf(quads)).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,net.minecraft.util.RandomSource random,ModelData data,net.minecraft.client.renderer.RenderType layer) {
        var result=new ArrayList<>(originalModel.getQuads(state,side,random,data,layer)); var drips=data.get(DRIPS); if (side==null && drips!=null) result.addAll(drips); return result;
    }
}
