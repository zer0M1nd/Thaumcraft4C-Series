package thmodern.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** The legacy provider has no sky, no sunrise and a fixed violet fog colour. */
public final class PocketPlaneEffects extends DimensionSpecialEffects {
    public PocketPlaneEffects() { super(Float.NaN, false, SkyType.NONE, false, false); }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 biomeColor, float daylight) {
        return new Vec3(0.02734375, 0.01171875, 0.16015625);
    }
    @Override public boolean isFoggyAt(int x, int z) { return false; }
    @Override public boolean renderSnowAndRain(net.minecraft.client.multiplayer.ClientLevel level,int ticks,float partialTick,net.minecraft.client.renderer.LightTexture light,double x,double y,double z){return true;}
    @Override public boolean tickRain(net.minecraft.client.multiplayer.ClientLevel level,int ticks,net.minecraft.client.Camera camera){return true;}
    @Override public float[] getSunriseColor(float time, float partialTick) { return null; }
}
