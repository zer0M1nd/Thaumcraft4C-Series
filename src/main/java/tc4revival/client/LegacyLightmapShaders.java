package tc4revival.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public  class LegacyLightmapShaders {
    static ShaderInstance nodeAlpha,nodeAdd,screen,warp,primalRays,researchNegative,eldritchFog;
    private LegacyLightmapShaders() {}
    @SubscribeEvent public static void register(RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:research_negative"),DefaultVertexFormat.POSITION_TEX_COLOR),shader->researchNegative=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:primal_rays"),DefaultVertexFormat.POSITION_COLOR),shader->primalRays=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:warp_post"),DefaultVertexFormat.POSITION_TEX),shader->warp=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:eldritch_fog"),DefaultVertexFormat.POSITION_TEX),shader->eldritchFog=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:node_alpha"),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),shader->nodeAlpha=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:node_add"),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),shader->nodeAdd=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:scanner_screen"),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),shader->screen=shader);
    }
}
