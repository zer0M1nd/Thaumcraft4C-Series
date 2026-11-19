package conciliummodern;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original six drifting leaf sprites; the particle atlas obtains the image from the original pack. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ConciliumLeafParticle extends TextureSheetParticle {
    private final double targetX,targetY,targetZ,driftX,driftY,driftZ;
    private final float rotationSpeed;private double sway;
    private ConciliumLeafParticle(ClientLevel level,double x,double y,double z,double tx,double ty,double tz,SpriteSet sprites){
        super(level,x,y,z);targetX=tx;targetY=ty;targetZ=tz;rotationSpeed=(random.nextBoolean()?1:-1)*.05f;roll=oRoll=random.nextFloat()*(float)Math.PI*2;
        xd=(random.nextDouble()*2-1)*.01;yd=(random.nextDouble()*2-1)*.01;zd=(random.nextDouble()*2-1)*.01;
        driftX=(random.nextDouble()*2-1)*.0525;driftY=(random.nextDouble()*2-1)*.0525;driftZ=(random.nextDouble()*2-1)*.0525;
        lifetime=180;gravity=0;hasPhysics=false;setSize(.01f,.01f);quadSize=(float)(.1*(1+random.nextGaussian()*.1));alpha=0;pickSprite(sprites);
    }
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){event.registerSpriteSet(RemainingMobs.LEAF.get(),sprites->(type,level,x,y,z,tx,ty,tz)->new ConciliumLeafParticle(level,x,y,z,x+tx,y+ty,z+tz,sprites));}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getLightColor(float partial){return 240;}
    @Override public void tick(){
        xo=x;yo=y;zo=z;oRoll=roll;roll+=rotationSpeed;float threshold=lifetime/5f;alpha=age<=threshold?age/threshold:(lifetime-age)/(float)lifetime;
        if(age++>=lifetime){remove();return;}double dx=targetX-x,dy=targetY-y,dz=targetZ-z,distance=dx*dx+dy*dy+dz*dz;if(distance<3.5){remove();return;}
        sway+=.05;xd+=driftX+Math.sin(sway*.7)*.0015;yd+=driftY+Math.cos(sway*.9)*.0012;zd+=driftZ+Math.sin(sway*1.3)*.0015;
        double speed=.12/Math.sqrt(distance);xd=dx*speed*.88;yd=dy*speed*.92;zd=dz*speed*.88;move(xd,yd,zd);
    }
}
