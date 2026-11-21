package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;

/** Animated silhouettes need row/column strips: a merged edge can sample a transparent
 * pixel in a later frame. The texture's current alpha cuts out each strip, as in TC4. */
public class AnimatedItemExtrusion extends BakedModelWrapper<BakedModel> {
    private final List<BakedQuad> quads;
    private AnimatedItemExtrusion(BakedModel original,List<BakedQuad> quads) { super(original); this.quads=List.copyOf(quads); }
    static BakedModel wrap(BakedModel original) {
        return wrap(original,true);
    }
    /** Legacy row/column extrusion also closes transparent edge samples on static displayed tools. */
    public static BakedModel forDisplay(BakedModel original) { return wrap(original,false); }
    private static BakedModel wrap(BakedModel original,boolean animatedOnly) {
        if (original.isCustomRenderer() || original instanceof AnimatedItemExtrusion) return original;
        var source=original.getQuads(null,null,RandomSource.create(0));
        var faces=source.stream().filter(q->q.getDirection().getAxis()==Direction.Axis.Z).toList();
        if(faces.size()!=2 || source.isEmpty())return original;
        var sprite=faces.get(0).getSprite();
        if(animatedOnly&&sprite.contents().getUniqueFrames().count()<2 || source.stream().anyMatch(q->q.getSprite()!=sprite))return original;
        // Restrict this to vanilla generated single-layer items; resource-pack 3D models stay intact.
        for(var face:faces) {
            int[] vertices=face.getVertices();int stride=vertices.length/4;
            float expected=face.getDirection()==Direction.NORTH?7.5f/16:8.5f/16;
            for(int i=0;i<4;i++)if(Math.abs(Float.intBitsToFloat(vertices[i*stride+2])-expected)>.00001f)return original;
        }
        var result=new ArrayList<>(faces);var bakery=new FaceBakery();
        int width=sprite.contents().width(),height=sprite.contents().height();
        for(int pixel=0;pixel<width;pixel++) {
            float low=pixel*16f/width,high=(pixel+1)*16f/width,u=(pixel+.5f)*16/width;
            strip(result,bakery,sprite,new Vector3f(low,0,7.5f),new Vector3f(low,16,8.5f),Direction.WEST,new float[]{u,0,u,16});
            strip(result,bakery,sprite,new Vector3f(high,0,7.5f),new Vector3f(high,16,8.5f),Direction.EAST,new float[]{u,0,u,16});
        }
        for(int pixel=0;pixel<height;pixel++) {
            float low=16-(pixel+1)*16f/height,high=16-pixel*16f/height,v=(pixel+.5f)*16/height;
            strip(result,bakery,sprite,new Vector3f(0,low,7.5f),new Vector3f(16,low,8.5f),Direction.DOWN,new float[]{0,v,16,v});
            strip(result,bakery,sprite,new Vector3f(0,high,7.5f),new Vector3f(16,high,8.5f),Direction.UP,new float[]{0,v,16,v});
        }
        return new AnimatedItemExtrusion(original,result);
    }
    private static void strip(List<BakedQuad> out,FaceBakery bakery,net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                              Vector3f from,Vector3f to,Direction side,float[] uv) {
        out.add(bakery.bakeQuad(from,to,new BlockElementFace(null,-1,"#layer0",new BlockFaceUV(uv,0)),sprite,side,
                BlockModelRotation.X0_Y0,null,true,sprite.contents().name()));
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random) { return side==null?quads:List.of(); }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type) { return getQuads(state,side,random); }
    @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) { originalModel.applyTransform(context,pose,left);return this; }
    @Override public List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous) { return List.of(this); }
    @Override public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model,ItemStack stack,ClientLevel level,LivingEntity entity,int seed) {
                var selected=originalModel.getOverrides().resolve(originalModel,stack,level,entity,seed);
                return selected==null||selected==originalModel?AnimatedItemExtrusion.this:wrap(selected);
            }
        };
    }
}
