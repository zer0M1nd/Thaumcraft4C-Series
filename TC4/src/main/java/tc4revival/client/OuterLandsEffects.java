package tc4revival.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public  class OuterLandsEffects extends DimensionSpecialEffects {
    public OuterLandsEffects() { super(Float.NaN,false,SkyType.NONE,false,false); }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float daylight) { return new Vec3(160/255.0*.15,128/255.0*.15,160/255.0*.15); }
    @Override public boolean isFoggyAt(int x,int z) { return true; }
    @Override public float[] getSunriseColor(float time,float partial) { return null; }
}
