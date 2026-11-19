package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The guardian refreshes a 200-tick mist, with the old density ramp and final 100-tick fade. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EldritchMist {
    private static ClientLevel current;
    private static int remaining;
    private static float density;
    private EldritchMist() {}
    private static void world() {
        var level=Minecraft.getInstance().level;
        if (current!=level) { current=level; remaining=0; density=0; }
    }
    public static void refresh() { world(); if (current!=null) remaining=Math.max(200,remaining); }
    public static void longMist() { world(); if (current!=null) remaining=2400; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; world();
        if (current==null || Minecraft.getInstance().isPaused() || remaining<=0) return;
        if (remaining<100) density=.1f*(remaining/100f);
        else if (density<.1f) density+=.001f;
        if (--remaining<=0) density=0;
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        world(); if (remaining<=0 || density<=0) return;
        // Use Forge's ordinary finite fog range. Encoding density in a large negative start
        // only works with our own global fog.glsl and breaks terrain shaders supplied by mods.
        // 95% extinction of the old exponential mist occurs at -log(.05) / density.
        var range = MistFogRange.apply(event.getNearPlaneDistance(), event.getFarPlaneDistance(), density);
        event.setNearPlaneDistance(range.near());
        event.setFarPlaneDistance(range.far());
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        event.setCanceled(true);
    }
}
