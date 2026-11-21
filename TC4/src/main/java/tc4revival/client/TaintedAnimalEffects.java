package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public  class TaintedAnimalEffects {
    private TaintedAnimalEffects() {}
    public static void appear(Entity entity) {
        int count=switch(Minecraft.getInstance().options.particles().get()) {case ALL->10;case DECREASED->5;case MINIMAL->0;};
        ThaumicSlimeParticles.sploosh(entity,count);
    }
    public static void explode(Entity entity) { TaintBottleEffects.burst(entity,100,false); }
}
