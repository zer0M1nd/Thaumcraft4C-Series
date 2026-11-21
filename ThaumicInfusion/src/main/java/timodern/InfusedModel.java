package timodern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/** Keep the host geometry/UV orientation; Sensus replaces face sprites and Fabrico multiplies all faces. */
public class InfusedModel extends BakedModelWrapper<BakedModel> {
    private final InfusionData.Entry infusion;
    private final BlockState disguise;
    public InfusedModel(BakedModel source,InfusionData.Entry e){super(source);infusion=e;disguise=e.has("sensus")&&e.settings.contains("disguise")?net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),e.settings.getCompound("disguise")):null;}
    public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource random,ModelData data,RenderType type){return transform(originalModel.getQuads(s,side,random,data,type),random);}
    public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource random){return transform(originalModel.getQuads(s,side,random),random);}
    private List<BakedQuad> transform(List<BakedQuad> source,RandomSource random){var out=new ArrayList<BakedQuad>(source.size());int tint=infusion.has("fabrico")&&infusion.settings.contains("tint")?infusion.settings.getInt("tint"):-1;var model=disguise==null?null:Minecraft.getInstance().getBlockRenderer().getBlockModel(disguise);
        for(var q:source){var sprite=q.getSprite();if(model!=null){var faces=model.getQuads(disguise,q.getDirection(),random);sprite=faces.isEmpty()?model.getParticleIcon():faces.get(0).getSprite();}int[] vertices=q.getVertices().clone();int stride=vertices.length/4;for(int n=0;n<4;n++){int offset=n*stride;if(sprite!=q.getSprite()){var old=q.getSprite();float u=(Float.intBitsToFloat(vertices[offset+4])-old.getU0())/(old.getU1()-old.getU0());float v=(Float.intBitsToFloat(vertices[offset+5])-old.getV0())/(old.getV1()-old.getV0());vertices[offset+4]=Float.floatToRawIntBits(sprite.getU(u*16));vertices[offset+5]=Float.floatToRawIntBits(sprite.getV(v*16));}if(tint!=-1){int c=vertices[offset+3];vertices[offset+3]=(c&0xff000000)|(((c&255)*(tint>>16&255)/255))|(((c>>8&255)*(tint>>8&255)/255)<<8)|(((c>>16&255)*(tint&255)/255)<<16);}}
            out.add(new BakedQuad(vertices,tint!=-1?-1:q.getTintIndex(),q.getDirection(),sprite,q.isShade()));}return out;
    }
}
