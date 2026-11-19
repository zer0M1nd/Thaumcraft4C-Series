package wgmodern;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/** Draws the copied state through the normal block pipeline, keeping its animation, tint and transparency. */
public class EtherealWallModel extends BakedModelWrapper<BakedModel> {
    public EtherealWallModel(BakedModel original){super(original);}
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){var camo=data.get(EtherealWall.Tile.CAMOUFLAGE);if(camo==null)return originalModel.getQuads(state,side,random,data,renderType);return Minecraft.getInstance().getBlockRenderer().getBlockModel(camo).getQuads(camo,side,random,ModelData.EMPTY,renderType);}
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){var camo=data.get(EtherealWall.Tile.CAMOUFLAGE);return camo==null?originalModel.getRenderTypes(state,random,data):Minecraft.getInstance().getBlockRenderer().getBlockModel(camo).getRenderTypes(camo,random,ModelData.EMPTY);}
    @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(ModelData data){var camo=data.get(EtherealWall.Tile.CAMOUFLAGE);return camo==null?originalModel.getParticleIcon(data):Minecraft.getInstance().getBlockRenderer().getBlockModel(camo).getParticleIcon(ModelData.EMPTY);}
}
