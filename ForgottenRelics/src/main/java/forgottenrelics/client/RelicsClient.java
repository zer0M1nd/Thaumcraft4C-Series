package forgottenrelics.client;

import com.mojang.blaze3d.platform.InputConstants;

import forgottenrelics.*;

import net.minecraft.client.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = ForgottenRelics.ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RelicsClient {
    private static final KeyMapping DISCORD =
            new KeyMapping(
                    "key.forgottenrelics.discord",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.forgottenrelics");

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(DISCORD);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RelicProjectiles.SPELL.get(), RelicProjectileRenderer::new);
    }

    public static void beam(RelicsNetwork.Beam message) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) return;
        var difference = message.to().subtract(message.from());
        double length = difference.length();
        if (!Double.isFinite(length) || length > 256) return;
        int steps = Math.max(1, Math.min(512, (int) (length * 8)));
        int color = message.color();
        var dust =
                new DustParticleOptions(
                        new org.joml.Vector3f(
                                ((color >> 16) & 255) / 255f,
                                ((color >> 8) & 255) / 255f,
                                (color & 255) / 255f),
                        .45f);
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        for (int i = 0; i <= steps; i++) {
            var position = message.from().add(difference.scale((double) i / steps));
            // A beam starts at the caster's eyes. Nearby dust would cover the whole view.
            if (position.distanceToSqr(camera) < 2.25) continue;
            double offset = Math.sin((double) i / steps * Math.PI) * .12;
            level.addParticle(
                    dust,
                    position.x + (level.random.nextDouble() - .5) * offset,
                    position.y + (level.random.nextDouble() - .5) * offset,
                    position.z + (level.random.nextDouble() - .5) * offset,
                    0,
                    0,
                    0);
        }
    }

    @Mod.EventBusSubscriber(modid = ForgottenRelics.ID, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            var mc = Minecraft.getInstance();
            if (event.phase == TickEvent.Phase.START) {
                // Vanilla drains attack clicks while using an item without firing the
                // interaction event. Handle those clicks before handleKeybinds does so.
                if (mc.player != null
                        && mc.screen == null
                        && mc.player.isUsingItem()
                        && Relics.is(mc.player.getUseItem(), Relic.TELEKINESIS_TOME)) {
                    while (mc.options.keyAttack.consumeClick())
                        RelicsNetwork.CHANNEL.sendToServer(new RelicsNetwork.Action(1));
                }
                return;
            }
            while (DISCORD.consumeClick())
                if (mc.player != null && mc.screen == null)
                    RelicsNetwork.CHANNEL.sendToServer(new RelicsNetwork.Action(0));
        }

        @SubscribeEvent
        public static void click(InputEvent.InteractionKeyMappingTriggered event) {
            var mc = Minecraft.getInstance();
            if (event.isAttack()
                    && mc.player != null
                    && mc.screen == null
                    && Relics.is(mc.player.getMainHandItem(), Relic.TELEKINESIS_TOME)) {
                event.setCanceled(true);
                event.setSwingHand(false);
                RelicsNetwork.CHANNEL.sendToServer(new RelicsNetwork.Action(1));
            }
        }

        @SubscribeEvent
        public static void movement(MovementInputUpdateEvent event) {
            if (event.getEntity().isUsingItem()
                    && Relics.is(event.getEntity().getUseItem(), Relic.SOUL_TOME)) {
                event.getInput().leftImpulse = 0;
                event.getInput().forwardImpulse = 0;
            }
            if (event.getEntity().isUsingItem()
                    && Relics.is(event.getEntity().getUseItem(), Relic.TELEKINESIS_TOME)
                    && !RelicsConfig.enabled("altTelekinesisAlgorithm")
                    && !event.getEntity().isPassenger()) {
                event.getInput().leftImpulse *= 5;
                event.getInput().forwardImpulse *= 5;
            }
        }
    }
}
