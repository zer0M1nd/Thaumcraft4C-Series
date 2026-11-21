package thaumcraft.api.client;

import net.minecraft.client.multiplayer.ClientLevel;

/** Client-only entry point. Keeps the host's distance, world-switch and particle-count limits. */
public final class ThaumicParticles {
    public record BubbleSpec(double x,double y,double z,int age,int color,int froth,double speed,float alpha) {
        public BubbleSpec {
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(speed)||!Float.isFinite(alpha))
                throw new IllegalArgumentException("Non-finite bubble parameter");
        }
    }
    private ThaumicParticles() {}
    public static void bubble(ClientLevel level,BubbleSpec spec){tc4revival.client.CrucibleEffects.spawnBubble(level,spec);}
}
