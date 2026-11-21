package tc4revival.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.TCEffects;

/** The old portal-layer vignette has frame-based easing, independent of shader configuration. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class WarpVignette {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/vignette.png");
    private static int remaining;
    private static float strength;
    private static boolean dark;
    private WarpVignette() {}
    public static void trigger(int ticks) { remaining=ticks; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        var mc=Minecraft.getInstance();
        if (mc.player==null || mc.level==null) { remaining=0; strength=0; dark=false; return; }
        if (mc.isPaused()) return;
        if (mc.player.hasEffect(TCEffects.DEATH_GAZE.get())) remaining=10;
        dark=remaining>0; if (dark) remaining--;
    }
    public static void render(GuiGraphics graphics,int width,int height) {
        var player=Minecraft.getInstance().player; if (player==null) return;
        strength=(float)(strength+((dark ? 1 : 0)-strength)*.01);
        if (strength<=0) return;
        float tint=strength*(1+Mth.sin(player.tickCount/2f)*.1f);
        graphics.flush(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO,GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ZERO);
        try {
            graphics.setColor(tint,tint,tint,1);
            graphics.blit(TEXTURE,0,0,0,0,width,height,width,height);
            graphics.flush();
        } finally {
            graphics.setColor(1,1,1,1); RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.defaultBlendFunc();
        }
    }
}
