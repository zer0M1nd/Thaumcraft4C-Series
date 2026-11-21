package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public  class InfernalFurnaceEffects {
    private InfernalFurnaceEffects() {}
    public static void smelted(Level level,BlockPos pos,Direction outlet) {
        var random=level.random;
        for (int i=0;i<5;i++) {
            double x=pos.getX()+.5+(random.nextFloat()-random.nextFloat())*.3+outlet.getStepX();
            double z=pos.getZ()+.5+(random.nextFloat()-random.nextFloat())*.3+outlet.getStepZ();
            var particle=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.LAVA,x,pos.getY()+.3,z,0,0,0);
            if (particle!=null) {
                double ySpeed=.2f*random.nextFloat();
                double xSpeed=outlet.getStepX()==0 ? (random.nextFloat()-random.nextFloat())*.5f : outlet.getStepX()*random.nextFloat();
                double zSpeed=outlet.getStepZ()==0 ? (random.nextFloat()-random.nextFloat())*.5f : outlet.getStepZ()*random.nextFloat();
                particle.setParticleSpeed(.15f*xSpeed,ySpeed,.15f*zSpeed);
            }
            level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.LAVA_POP,SoundSource.BLOCKS,
                    .1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
        }
    }
}
