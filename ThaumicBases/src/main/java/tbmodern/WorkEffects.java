package tbmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.fx.LegacyEffects;

/** Effects are emitted once by the server and distributed to nearby observers. */
public final class WorkEffects {
    public static void sound(Level level,BlockPos pos,String id,float volume,float pitch) {
        var sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("thaumcraft",id));
        if(sound!=null)level.playSound(null,pos,sound,SoundSource.BLOCKS,volume,pitch);
    }
    public static void lightning(Level level,Vec3 from,Vec3 to,int color) {
        tc4revival.network.FocusEffectPacket.lightning(level,from,to);
    }
    public static void manipulator(Level level,BlockPos pos,String mode) {
        int color=switch(mode) {
            case "destruction"->0x4e4756;case "efficiency"->0xd2d200;case "hunger"->0xaf7c23;
            case "instability"->0x0b4d42;case "purity"->0xccc8f7;case "sinister"->0x643c5b;
            case "speed"->0xeaeaea;case "stability"->0xd0e0f8;case "taint"->0x713496;default->0xffffff;
        };
        LegacyEffects.beam(level,pos.getCenter(),pos.below().getCenter(),color,.5F,8);
    }
    public static void link(Level level,BlockPos receiver,BlockPos donor) {
        Vec3 from=receiver.getCenter().add(0,.325,0),to=donor.getCenter().add(0,.325,0);
        LegacyEffects.beam(level,from,to,0xffffff,.5F,8);
        tc4revival.network.FocusEffectPacket.nodeZap(level,receiver.below().getCenter(),from);
        tc4revival.network.FocusEffectPacket.nodeZap(level,donor.below().getCenter(),to);
    }
    private WorkEffects() {}
}
