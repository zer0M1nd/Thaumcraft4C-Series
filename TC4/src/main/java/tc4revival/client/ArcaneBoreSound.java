package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import tc4revival.content.ArcaneBoreBlockEntity;
import tc4revival.content.TCSounds;

/** The original rumble is stereo: OpenAL does not spatially attenuate stereo buffers. */
public class ArcaneBoreSound extends AbstractTickableSoundInstance {
    private final ArcaneBoreBlockEntity bore;
    private ArcaneBoreSound(ArcaneBoreBlockEntity bore,float pitch) {
        super(TCSounds.RUMBLE.get(),SoundSource.BLOCKS,RandomSource.create());
        this.bore=bore;this.pitch=pitch;attenuation=Attenuation.NONE;
        var center=bore.getBlockPos().getCenter();x=center.x;y=center.y;z=center.z;
        updateVolume();
    }
    public static float volumeAtDistance(double distance) { return .25f*(float)Math.max(0,1-distance/16); }
    private void updateVolume(){volume=volumeAtDistance(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(bore.getBlockPos().getCenter()));}
    public static void play(ArcaneBoreBlockEntity bore,float pitch){
        var sound=new ArcaneBoreSound(bore,pitch);
        if(sound.volume>0)Minecraft.getInstance().getSoundManager().play(sound);
    }
    @Override public void tick(){
        if(bore.isRemoved()||bore.getLevel()!=Minecraft.getInstance().level){stop();return;}
        updateVolume();if(volume<=0)stop();
    }
}
