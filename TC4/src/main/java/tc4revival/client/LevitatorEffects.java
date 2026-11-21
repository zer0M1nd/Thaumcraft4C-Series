package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public  class LevitatorEffects {
    private LevitatorEffects() {}
    public static boolean shiftDown() { return net.minecraft.client.gui.screens.Screen.hasShiftDown(); }
    public static void sparkle(Level level,BlockPos pos,RandomSource random) {
        ClientFocusEffects.levitatorSparkle(level,new Vec3(pos.getX()+.2f+random.nextFloat()*.6f,pos.getY()+1,pos.getZ()+.2f+random.nextFloat()*.6f));
    }
}
