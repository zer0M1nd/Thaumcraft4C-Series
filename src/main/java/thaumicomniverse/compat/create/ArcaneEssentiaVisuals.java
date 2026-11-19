package thaumicomniverse.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import tc4revival.aspect.Aspect;
import tc4revival.client.TCRenderTypes;

public class ArcaneEssentiaVisuals {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:block/animatedglow");
    public static void liquid(Aspect aspect,PoseStack pose,MultiBufferSource buffers,float x0,float y0,float z0,float x1,float y1,float z1){
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(TEXTURE);
        var out=buffers.getBuffer(TCRenderTypes.ESSENTIA_LIQUID);int color=aspect.color();
        float[][] vertices={{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1},{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}};
        int[][] faces={{0,1,2,3},{7,6,5,4},{1,0,4,5},{3,2,6,7},{0,3,7,4},{2,1,5,6}};
        for(int f=0;f<faces.length;f++)for(int corner:faces[f]){
            var p=vertices[corner];
            // The stream extends below its block. Keep all UVs inside the sprite,
            // instead of sampling neighbouring atlas textures at negative heights.
            float u=f<4?(p[0]-x0)/(x1-x0):(p[2]-z0)/(z1-z0);
            float v=f<2?(p[2]-z0)/(z1-z0):(y1-p[1])/(y1-y0);
            out.vertex(pose.last().pose(),p[0],p[1],p[2]).color(color>>16&255,color>>8&255,color&255,255).uv(sprite.getU(u*16),sprite.getV(v*16)).uv2(0xf000f0).endVertex();
        }
    }
    public static class Drop extends TextureSheetParticle {
        public Drop(ClientLevel level,double x,double y,double z,Aspect aspect){
            super(level,x,y,z);setSprite(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(TEXTURE));
            int c=aspect.color();setColor((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f);
            quadSize=.04f;lifetime=12;gravity=.5f;xd=(random.nextDouble()-.5)*.02;yd=-.06;zd=(random.nextDouble()-.5)*.02;
        }
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.TERRAIN_SHEET;}
        @Override protected int getLightColor(float partial){return 0xf000f0;}
    }
}
