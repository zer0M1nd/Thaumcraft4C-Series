package automagy.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import tc4revival.client.EssentiaVentParticles;

/** Original four outlets, ten client ticks, using TC4's expanding translucent vent sprites. */
public class BoilerEffects {
    public static void vent(Level level,BlockPos pos){
        float dx=.1f-level.random.nextFloat()*.2f,dz=.1f-level.random.nextFloat()*.2f,dy=2+level.random.nextFloat()*5;
        double x=pos.getX(),y=pos.getY()+.85,z=pos.getZ();
        EssentiaVentParticles.emit(level,x+.85,y,z+.1,2+dx,dy,-2+dz,0xffffff);
        EssentiaVentParticles.emit(level,x+.15,y,z+.1,-2+dx,dy,-2+dz,0xffffff);
        // Preserve the original south-east outlet's extra directional compensation.
        EssentiaVentParticles.emit(level,x+.85,y,z+.9,4+dx,dy,4+dz,0xffffff);
        EssentiaVentParticles.emit(level,x+.15,y,z+.9,-2+dx,dy,2+dz,0xffffff);
    }
    private BoilerEffects(){}
}
