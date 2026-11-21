package tc4revival.content;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Legacy throwable launch offsets and Gaussian dispersion, before modern projectile ticking takes over. */
 class EldritchProjectileLaunch {
    private EldritchProjectileLaunch() {}
    static void initialize(Projectile projectile,LivingEntity owner) {
        float yaw=owner.getYRot()/180*(float)Math.PI,pitch=owner.getXRot()/180*(float)Math.PI;
        projectile.setOwner(owner); projectile.setYRot(owner.getYRot()); projectile.setXRot(owner.getXRot());
        projectile.setPos(owner.getX()-Mth.cos(yaw)*.16f,owner.getEyeY()-(double).1f,owner.getZ()-Mth.sin(yaw)*.16f);
        projectile.shoot(-Mth.sin(yaw)*Mth.cos(pitch)*.4f,-Mth.sin(pitch)*.4f,Mth.cos(yaw)*Mth.cos(pitch)*.4f,1.5f,1);
    }
    static void shoot(Projectile projectile,RandomSource random,double x,double y,double z,float speed,float spread) {
        float length=(float)Math.sqrt(x*x+y*y+z*z);
        if (!(length>1e-12f) || !Float.isFinite(length)) { projectile.setDeltaMovement(Vec3.ZERO); return; }
        double noise=(double).0075f*spread;
        Vec3 velocity=new Vec3(x/length+random.nextGaussian()*noise,y/length+random.nextGaussian()*noise,z/length+random.nextGaussian()*noise).scale(speed);
        projectile.setDeltaMovement(velocity);
        float yaw=(float)(Math.atan2(velocity.x,velocity.z)*180/(double)(float)Math.PI);
        float pitch=(float)(Math.atan2(velocity.y,(float)Math.sqrt(velocity.x*velocity.x+velocity.z*velocity.z))*180/(double)(float)Math.PI);
        projectile.setYRot(yaw); projectile.setXRot(pitch); projectile.yRotO=yaw; projectile.xRotO=pitch;
    }
}
