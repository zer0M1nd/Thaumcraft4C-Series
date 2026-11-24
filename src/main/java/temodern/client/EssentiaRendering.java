package temodern.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import appeng.api.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tc4revival.aspect.Aspect;
import tc4revival.client.LegacyGui;
import temodern.TE;
import temodern.essentia.*;

@Mod.EventBusSubscriber(modid = TE.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EssentiaRendering implements AEKeyRenderHandler<EssentiaKey> {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(temodern.crafting.WorkMenu.GENERATOR.get(), GeneratorScreen::new));
        event.enqueueWork(() -> AEKeyRendering.register(EssentiaKeyType.INSTANCE, EssentiaKey.class, new EssentiaRendering()));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(temodern.crafting.WorkMenu.TYPE.get(), WorkScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(temodern.crafting.WorkMenu.INSCRIBER.get(), InscriberScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(temodern.crafting.WorkMenu.TERMINAL.get(), ArcaneTerminalScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(temodern.crafting.WorkMenu.ASSEMBLER.get(), ArcaneAssemblerScreen::new));
    }
    @Override public void drawInGui(Minecraft minecraft, GuiGraphics graphics, int x, int y, EssentiaKey key) {
        var aspect = key.aspect();
        if (aspect != null) drawAspect(graphics, x, y, 16, aspect);
    }

    public static void drawAspect(GuiGraphics graphics, int x, int y, int size, Aspect aspect) {
        // Flush pending items before tinting. LegacyGui enables blending for the translucent texture edges.
        graphics.flush();
        int rgb = aspect.color();
        graphics.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
        LegacyGui.blit(graphics, aspect.texture(), x, y, size, size, 0, 0, 16, 16, 16, 16);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @Override public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, EssentiaKey key, float scale, int light, Level level) {
        var aspect = key.aspect();
        if (aspect == null) return;
        var vertex = buffers.getBuffer(RenderType.entityCutoutNoCull(aspect.texture()));
        var matrix = pose.last().pose(); var normal = pose.last().normal();
        float r = ((aspect.color() >> 16) & 255) / 255f, g = ((aspect.color() >> 8) & 255) / 255f, b = (aspect.color() & 255) / 255f;
        float size = scale / 2;
        vertex.vertex(matrix, -size, -size, 0).color(r,g,b,1).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,0,0,1).endVertex();
        vertex.vertex(matrix, size, -size, 0).color(r,g,b,1).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,0,0,1).endVertex();
        vertex.vertex(matrix, size, size, 0).color(r,g,b,1).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,0,0,1).endVertex();
        vertex.vertex(matrix, -size, size, 0).color(r,g,b,1).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,0,0,1).endVertex();
    }
    @Override public Component getDisplayName(EssentiaKey key) { return key.getDisplayName(); }
}
