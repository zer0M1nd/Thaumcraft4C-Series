package thmodern.client;

import java.util.IdentityHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.plane.VortexEntity;

/** One managed loop per visible rift; removal, world changes and reloads release it. */
@Mod.EventBusSubscriber(modid=Horizons.ID, value=Dist.CLIENT)
public final class VortexAudio {
    private static final IdentityHashMap<VortexEntity, Loop> LOOPS=new IdentityHashMap<>();
    public static void ensure(VortexEntity vortex) {
        if(LOOPS.containsKey(vortex)) return;
        var loop=new Loop(vortex);
        LOOPS.put(vortex,loop);
        Minecraft.getInstance().getSoundManager().play(loop);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        LOOPS.entrySet().removeIf(entry->{
            var loop=entry.getValue();
            if(loop.valid()&&!loop.isStopped()) return false;
            Minecraft.getInstance().getSoundManager().stop(loop);
            return true;
        });
    }
    public static void clear() {
        LOOPS.values().forEach(Minecraft.getInstance().getSoundManager()::stop);
        LOOPS.clear();
    }
    private static final class Loop extends AbstractTickableSoundInstance {
        private final VortexEntity vortex;
        Loop(VortexEntity vortex) {
            super(Horizons.VORTEX_SOUND.get(),SoundSource.AMBIENT,SoundInstance.createUnseededRandom());
            this.vortex=vortex; looping=true; delay=0; volume=1;
            attenuation=Attenuation.NONE;
            x=vortex.getBlockPos().getX()+.5;
            y=vortex.getBlockPos().getY()+.5;
            z=vortex.getBlockPos().getZ()+.5;
        }
        boolean valid() {
            var client=Minecraft.getInstance();
            return client.player!=null&&client.level!=null&&vortex.getLevel()==client.level
                &&!vortex.isRemoved()&&client.level.hasChunkAt(vortex.getBlockPos())
                &&client.level.getBlockEntity(vortex.getBlockPos())==vortex
                &&client.player.distanceToSqr(x,y,z)<64*64;
        }
        @Override public void tick() {
            if(!valid()){stop();return;}
            volume=1/(1+(float)Math.sqrt(Minecraft.getInstance().player.distanceToSqr(x,y,z))/8);
        }
        @Override public boolean canStartSilent(){return true;}
    }
}
