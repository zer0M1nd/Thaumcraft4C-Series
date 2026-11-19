package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.config.TCConfig;
import tc4revival.content.TCEffects;

/** Ordered independent chains matching the old four status-effect passes. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class WarpPostEffects {
    private static final String[] NAMES={"desaturatetc","blurred_vision","hunger","sunscorned"};
    private static final State[] STATES={new State(),new State(),new State(),new State()};
    private static  class State { PostChain chain; WarpScreenPass screen; int width,height; boolean failed; }
    private WarpPostEffects() {}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) {
        for (var state:STATES) { close(state); state.failed=false; }
    }
    private static boolean active(int index) {
        var mc=Minecraft.getInstance();
        return mc.level!=null && mc.player!=null && TCConfig.CLIENT.shaders.get() && mc.player.hasEffect(switch(index) { case 0 -> TCEffects.DEATH_GAZE.get(); case 1 -> TCEffects.BLURRED_VISION.get(); case 2 -> TCEffects.UNNATURAL_HUNGER.get(); default -> TCEffects.SUN_SCORNED.get(); });
    }
    private static void close(State state) {
        if (state.chain!=null) { state.chain.close(); state.chain=null; }
        if (state.screen!=null) { state.screen.close(); state.screen=null; }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase==TickEvent.Phase.END) for (int i=0;i<STATES.length;i++) if (!active(i)) close(STATES[i]);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Pre event) {
        for (int i=0;i<STATES.length;i++) render(event,i);
    }
    private static void render(RenderGuiEvent.Pre event,int index) {
        var state=STATES[index];
        if (!active(index)) { close(state); return; }
        if (state.failed) return;
        var mc=Minecraft.getInstance(); var target=mc.getMainRenderTarget();
        var previousShader=com.mojang.blaze3d.systems.RenderSystem.getShader();
        int previousTexture=com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0);
        boolean previousDepthMask=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
        boolean previousBlend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        try {
            event.getGuiGraphics().flush();
            if(index==1 || index==3) {
                if(state.screen==null) state.screen=new WarpScreenPass();
                state.screen.process(target,index==3);
                return;
            }
            if (state.chain==null) {
                state.chain=new PostChain(mc.getTextureManager(),mc.getResourceManager(),target,ResourceLocation.parse("thaumcraft:shaders/post/"+NAMES[index]+".json"));
                state.width=state.height=-1;
            }
            if (state.width!=target.width || state.height!=target.height) {
                state.chain.resize(target.width,target.height); state.width=target.width; state.height=target.height;
            }
            event.getGuiGraphics().flush();
            // Match GameRenderer's post-chain setup; the GUI pass does not supply these states.
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.disableCull();
            com.mojang.blaze3d.systems.RenderSystem.resetTextureMatrix();
            state.chain.process(event.getPartialTick());
        } catch (Exception error) {
            close(state); state.failed=true;
            com.mojang.logging.LogUtils.getLogger().warn("Unable to render TC4 post effect {} ; retrying after resource reload",NAMES[index],error);
        } finally {
            target.bindWrite(true);
            // ShaderInstance/EffectInstance share BlendMode's cache. Prime the GUI blend
            // mode before restoring GL state, otherwise the next position_tex shader
            // overwrites the vignette's ZERO/ONE_MINUS_SRC_COLOR factors with alpha blend.
            new com.mojang.blaze3d.shaders.BlendMode(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,org.lwjgl.opengl.GL14.GL_FUNC_ADD).apply();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(previousDepthMask);
            com.mojang.blaze3d.systems.RenderSystem.setShader(()->previousShader);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,previousTexture);
            com.mojang.blaze3d.systems.RenderSystem.enableCull();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            if(previousBlend) com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            else com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
}

