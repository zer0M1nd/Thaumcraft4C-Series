package tc4revival.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.*;
import tc4revival.content.TaintFibreAppearance;
import tc4revival.content.TaintFibreBlock;

/** Surface fibres remain on growth variants; only bare fibres have the rare luminous overlay. */
public  class TaintFibreModel extends BakedModelWrapper<BakedModel> {
    private record Appearance(BlockPos pos,BlockState state,boolean floor,List<BakedQuad> overlay) {}
    private static final ModelProperty<Appearance> APPEARANCE=new ModelProperty<>();
    public TaintFibreModel(BakedModel original) { super(original); }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean useAmbientOcclusion(BlockState state) { return false; }
    @Override public boolean useAmbientOcclusion(BlockState state,RenderType layer) { return false; }
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData previous) {
        state=TaintFibreBlock.faces(state,level,pos);
        var quads=new ArrayList<BakedQuad>();
        if(state.getValue(TaintFibreBlock.VARIANT)==0 && tc4revival.config.TCConfig.CLIENT.glowingTaint.get()) {
            for(var side:Direction.values()) {
                if(!state.getValue(MultifaceBlock.getFaceProperty(side))) continue;
                int texture=TaintFibreAppearance.overlay(pos.getX(),pos.getY(),pos.getZ(),side.get3DDataValue());
                if(texture==0)continue;
                var id=ResourceLocation.parse("thaumcraft:block/taint_over_"+texture);
                var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
                var from=new org.joml.Vector3f(0,0,0);var to=new org.joml.Vector3f(16,16,16);
                int axis=side.getAxis()==Direction.Axis.X?0:side.getAxis()==Direction.Axis.Y?1:2;
                float coordinate=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE?.32f:15.68f;
                from.setComponent(axis,coordinate);to.setComponent(axis,coordinate);
                var face=side.getOpposite();
                var faces=new EnumMap<Direction,BlockElementFace>(Direction.class);
                faces.put(face,new BlockElementFace(null,-1,"#all",new BlockFaceUV(null,0)));
                var element=new BlockElement(from,to,faces,null,false);
                var quad=new FaceBakery().bakeQuad(from,to,element.faces.get(face),sprite,face,BlockModelRotation.X0_Y0,null,false,id);
                int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;
                for(int i=0;i<4;i++)vertices[i*stride+6]=200;
                quads.add(new BakedQuad(vertices,-1,face,sprite,false,false));
            }
        }
        var below=pos.below();boolean floor=level.getBlockState(below).isFaceSturdy(level,below,Direction.UP);
        return originalModel.getModelData(level,pos,state,previous).derive().with(APPEARANCE,new Appearance(pos.immutable(),state,floor,List.copyOf(quads))).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType layer) {
        var appearance=data.get(APPEARANCE);
        if(appearance!=null)state=appearance.state();
        var source=originalModel.getQuads(state,side,random,data,layer);
        if(appearance==null||state==null)return source;
        int kind=state.getValue(TaintFibreBlock.VARIANT);var result=new ArrayList<BakedQuad>();
        double[] offset=TaintFibreAppearance.plantOffset(appearance.pos().getX(),appearance.pos().getY(),appearance.pos().getZ());
        for(var quad:source) {
            String texture=quad.getSprite().contents().name().getPath();
            boolean cross=texture.endsWith("taintgrass1")||texture.endsWith("taintgrass2")||texture.endsWith("taint_spore_stalk_1")||texture.endsWith("taint_spore_stalk_2");
            if(cross&&!TaintFibreAppearance.hasCross(kind,appearance.floor()))continue;
            if(cross&&(kind==1||kind==2)) {
                int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;
                for(int i=0;i<4;i++) {
                    vertices[i*stride]=Float.floatToRawIntBits(Float.intBitsToFloat(vertices[i*stride])+(float)offset[0]);
                    vertices[i*stride+2]=Float.floatToRawIntBits(Float.intBitsToFloat(vertices[i*stride+2])+(float)offset[1]);
                }
                result.add(new BakedQuad(vertices,0,quad.getDirection(),quad.getSprite(),false,false));
            } else result.add(new BakedQuad(quad.getVertices(),0,quad.getDirection(),quad.getSprite(),false,false));
        }
        if(side==null&&(layer==null||layer==RenderType.translucent()))result.addAll(appearance.overlay());
        return result;
    }
}
