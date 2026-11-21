package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** TC4 GUI textures carry partial alpha. Item/font batches may disable blending on completion. */
public  class LegacyGui {
    private LegacyGui() {}
    private static void prepare(GuiGraphics graphics) {
        // Submit pending items/text before setting state for the immediate texture draw.
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
    public static void blit(GuiGraphics g, ResourceLocation texture,int x,int y,int u,int v,int width,int height) {
        prepare(g);g.blit(texture,x,y,u,v,width,height);
    }
    public static void blit(GuiGraphics g, ResourceLocation texture,int x,int y,float u,float v,int width,int height,int textureWidth,int textureHeight) {
        prepare(g);g.blit(texture,x,y,u,v,width,height,textureWidth,textureHeight);
    }
    public static void blit(GuiGraphics g, ResourceLocation texture,int x,int y,int width,int height,float u,float v,int regionWidth,int regionHeight,int textureWidth,int textureHeight) {
        prepare(g);g.blit(texture,x,y,width,height,u,v,regionWidth,regionHeight,textureWidth,textureHeight);
    }
    public static void blit(GuiGraphics g,int x,int y,int z,int width,int height,TextureAtlasSprite sprite) {
        prepare(g);g.blit(x,y,z,width,height,sprite);
    }
}
