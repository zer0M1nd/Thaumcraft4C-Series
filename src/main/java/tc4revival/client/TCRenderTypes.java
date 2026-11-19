package tc4revival.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Legacy texture sampling, lightmap and blending expressed through modern render state. */
public  class TCRenderTypes extends RenderType {
    private static final java.util.Map<RenderType,Boolean> SPRITES = new java.util.IdentityHashMap<>();

    public static com.mojang.blaze3d.vertex.VertexConsumer buffer(net.minecraft.client.renderer.MultiBufferSource buffers, RenderType type) {
        Boolean sort = SPRITES.get(type);
        return sort != null && ShaderPackMagicRendering.shouldDefer()
                ? ShaderPackMagicRendering.buffer(type, sort) : buffers.getBuffer(type);
    }

    public static void endBatch(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers, RenderType type) {
        if (!SPRITES.containsKey(type) || !ShaderPackMagicRendering.shouldDefer()) buffers.endBatch(type);
    }

    private static RenderType createSprite(String name, VertexFormat format, VertexFormat.Mode mode,
            int size, boolean crumbling, boolean sort, CompositeState state) {
        var type = create(name,format,mode,size,crumbling,sort,state);
        SPRITES.put(type,sort);
        return type;
    }

    private static final ShaderStateShard PACK_LIGHTMAP_SHADER = new ShaderStateShard(
            () -> ShaderPackShaders.lightmap(net.minecraft.client.renderer.GameRenderer::getPositionColorTexLightmapShader));
    // ShaderInstance.apply() also applies the shader JSON's blend function. The
    // vanilla lightmap shader restores alpha blending after our additive state,
    // so it cannot be used for stacked portal layers or luminous particles.
    private static final ShaderStateShard PACK_ADDITIVE_SHADER = new ShaderStateShard(
            () -> ShaderPackShaders.lightmap(() -> LegacyLightmapShaders.nodeAdd));
    private static TextureStateShard smoothTexture(ResourceLocation texture) {
        // Linear filtering is appropriate for standalone TC sprites, never the shared block atlas.
        // TextureStateShard changes the texture object permanently; fire overlays reuse that atlas
        // without resetting its sampler and otherwise inherit blurred magnification.
        boolean atlas=texture.equals(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        return new TextureStateShard(texture,!atlas,atlas);
    }
    private TCRenderTypes() { super("unused", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {}); }
    private static final Function<RenderType,RenderType> DECONSTRUCTION_ITEM=Util.memoize(original->new RenderType("thaumcraft_deconstruction_item",original.format(),original.mode(),original.bufferSize(),original.affectsCrumbling(),true,
            ()->{ original.setupRenderState(); com.mojang.blaze3d.systems.RenderSystem.enableBlend(); com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE); },
            ()->{ com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc(); original.clearRenderState(); }) {});
    public static RenderType deconstructionItem(RenderType original) { return DECONSTRUCTION_ITEM.apply(original); }
    private static final Function<RenderType,RenderType> RESEARCH_MIRROR_ITEM=Util.memoize(original->new RenderType("thaumcraft_research_mirror",original.format(),original.mode(),original.bufferSize(),original.affectsCrumbling(),true,
            ()->{original.setupRenderState();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();},
            original::clearRenderState) {});
    public static RenderType researchMirrorItem(RenderType original){return RESEARCH_MIRROR_ITEM.apply(original);}

    private static final Function<ResourceLocation,RenderType> ELDRITCH_EYE=Util.memoize(texture->create("thaumcraft_eldritch_eye",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType eldritchEye(ResourceLocation texture) { return ELDRITCH_EYE.apply(texture); }
    public static final RenderType PRIMAL_RAYS=createSprite("thaumcraft_primal_rays",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.TRIANGLES,1024,false,false,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->ShaderPackShaders.rays(()->LegacyLightmapShaders.primalRays))).setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final Function<ResourceLocation,RenderType> PRIMAL_SPRITE=Util.memoize(texture->createSprite("thaumcraft_primal_sprite",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,false,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->ShaderPackShaders.lightmap(()->LegacyLightmapShaders.nodeAdd)))
                    .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType primalSprite(ResourceLocation texture) {return PRIMAL_SPRITE.apply(texture);}
    private static final Function<ResourceLocation, RenderType> RUNES = Util.memoize(texture -> additive(texture, false));
    private static final Function<ResourceLocation,RenderType> SOFT_PARTICLE=Util.memoize(texture->createSprite("thaumcraft_soft_particle",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder()
                    .setShaderState(PACK_LIGHTMAP_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    private static final Function<ResourceLocation,RenderType> BLOOM_PLANT=Util.memoize(texture->createSprite("thaumcraft_bloom_plant",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder()
            .setShaderState(PACK_LIGHTMAP_SHADER).setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false)));
    public static RenderType bloomPlant(ResourceLocation texture){return BLOOM_PLANT.apply(texture);}
    public static RenderType softParticle(ResourceLocation texture) { return SOFT_PARTICLE.apply(texture); }
    private static final Function<ResourceLocation,RenderType> INFUSION_RUNES=Util.memoize(texture->create("thaumcraft_infusion_runes",
            DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType infusionRunes(ResourceLocation texture) { return INFUSION_RUNES.apply(texture); }
    public static final RenderType ESSENTIA_LIQUID=create("thaumcraft_essentia_liquid",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder().setShaderState(PACK_LIGHTMAP_SHADER)
                    .setTextureState(new TextureStateShard(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,false,false))
                    .setLightmapState(LIGHTMAP).setCullState(NO_CULL).createCompositeState(false));
    private static final Function<ResourceLocation, RenderType> SOFT_PARTICLES = Util.memoize(texture -> createSprite("thaumcraft_soft_particles",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(PACK_ADDITIVE_SHADER).setTextureState(smoothTexture(texture))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType softParticles(ResourceLocation texture) { return SOFT_PARTICLES.apply(texture); }
    private static final Function<ResourceLocation, RenderType> SOFT_TRANSLUCENT = Util.memoize(texture -> createSprite("thaumcraft_soft_translucent",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(PACK_LIGHTMAP_SHADER).setTextureState(smoothTexture(texture))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType softTranslucent(ResourceLocation texture) { return SOFT_TRANSLUCENT.apply(texture); }
    private static final Function<ResourceLocation,RenderType> SHARP_TRANSLUCENT=Util.memoize(texture->createSprite("thaumcraft_sharp_translucent",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder()
                    .setShaderState(PACK_LIGHTMAP_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType sharpTranslucent(ResourceLocation texture) { return SHARP_TRANSLUCENT.apply(texture); }
    private static final Function<ResourceLocation,RenderType> MIRROR_PANE=Util.memoize(texture->createSprite("thaumcraft_mirror_pane",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
                    .setShaderState(new ShaderStateShard(()->ShaderPackShaders.lightmap(()->LegacyLightmapShaders.nodeAlpha)))
                    .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType mirrorPane(ResourceLocation texture) { return MIRROR_PANE.apply(texture); }
    private static final Function<ResourceLocation,RenderType> WORLD_TRANSLUCENT=Util.memoize(texture->create("thaumcraft_world_translucent",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder()
                    .setShaderState(PACK_LIGHTMAP_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false)));
    public static RenderType worldTranslucent(ResourceLocation texture) { return WORLD_TRANSLUCENT.apply(texture); }
    private static final Function<ResourceLocation,RenderType> GHOST_MODEL=Util.memoize(texture->create("thaumcraft_ghost_model",
            DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType ghostModel(ResourceLocation texture) { return GHOST_MODEL.apply(texture); }
    private static final java.util.function.BiFunction<ResourceLocation, Boolean, RenderType> REVEALED_NODE = Util.memoize((texture, translucent) -> createSprite("thaumcraft_revealed_node",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, translucent, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> ShaderPackShaders.lightmap(() -> translucent ? LegacyLightmapShaders.nodeAlpha : LegacyLightmapShaders.nodeAdd))).setTextureState(smoothTexture(texture))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : LIGHTNING_TRANSPARENCY).setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    // Use the same shader-pack-aware world overlay pipeline as the adjacent count text.
    public static RenderType revealedAspect(ResourceLocation texture){return RenderType.textSeeThrough(texture);}
    public static RenderType revealedNode(ResourceLocation texture, boolean translucent) { return REVEALED_NODE.apply(texture, translucent); }
    private static final java.util.function.BiFunction<ResourceLocation,Boolean,RenderType> NODE=Util.memoize((texture,translucent)->createSprite("thaumcraft_node",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,translucent,CompositeState.builder()
                    .setShaderState(new ShaderStateShard(()->ShaderPackShaders.lightmap(()->translucent?LegacyLightmapShaders.nodeAlpha:LegacyLightmapShaders.nodeAdd)))
                    .setTextureState(smoothTexture(texture)).setLightmapState(LIGHTMAP).setCullState(NO_CULL)
                    .setTransparencyState(translucent?TRANSLUCENT_TRANSPARENCY:LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static RenderType node(ResourceLocation texture,boolean translucent) { return NODE.apply(texture,translucent); }
    private static final Function<ResourceLocation, RenderType> LENS = Util.memoize(texture -> additive(texture, true));
    private static final Function<ResourceLocation, RenderType> SCREEN = Util.memoize(texture -> createSprite("thaumcraft_scanner_screen",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> ShaderPackShaders.lightmap(() -> LegacyLightmapShaders.screen)))
                    // entityTranslucent forces nearest sampling, overriding the legacy blur metadata.
                    .setTextureState(smoothTexture(texture))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP).createCompositeState(true)));
    public static RenderType runes(ResourceLocation texture) { return RUNES.apply(texture); }
    private static final Function<ResourceLocation, RenderType> ORB = Util.memoize(texture -> createSprite("thaumcraft_aspect_orb",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(PACK_LIGHTMAP_SHADER).setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false)));
    public static RenderType orb(ResourceLocation texture) { return ORB.apply(texture); }
    public static RenderType lens(ResourceLocation texture) { return LENS.apply(texture); }
    public static RenderType screen(ResourceLocation texture) { return SCREEN.apply(texture); }
    private static RenderType additive(ResourceLocation texture, boolean depthWrite) {
        return createSprite("thaumcraft_additive_" + (depthWrite ? "lens" : "runes"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                        .setShaderState(PACK_ADDITIVE_SHADER).setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                        .setWriteMaskState(depthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE).createCompositeState(false));
    }
}
