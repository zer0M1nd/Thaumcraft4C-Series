package emtmodern.focus;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
public class LaserEntity extends Projectile {
    public float strength=2;
    protected boolean inGround;
    protected int airTicks;
    public LaserEntity(EntityType<? extends LaserEntity> type,Level level){super(type,level);}
    public void fire(LivingEntity owner){setOwner(owner);double yaw=Math.toRadians(owner.getYRot());setPos(owner.getX()-Math.cos(yaw)*.16,owner.getEyeY()-.1,owner.getZ()-Math.sin(yaw)*.16);setYRot(owner.getYRot());setXRot(owner.getXRot());setDeltaMovement(owner.getLookAngle());}
    @Override protected void defineSynchedData(){}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<256*256;}
    public void explode(){if(!level().isClientSide){
        // The author's laser extends Entity, not EntityLivingBase/TNT. Its legacy
        // explosion therefore has no attacker; modern Projectile infers an owner.
        var damage=level().damageSources().explosion(null,null);
        level().explode(this,damage,null,(int)getX(),(int)getY(),(int)getZ(),strength,false,Level.ExplosionInteraction.TNT);
        discard();
    }}
    @Override public void tick(){
        super.tick();if(inGround){explode();return;}airTicks++;
        Vec3 from=position(),motion=getDeltaMovement(),end=from.add(motion);var block=level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));Vec3 rayEnd=block.getType()==HitResult.Type.MISS?end:block.getLocation();
        Entity target=null;double closest=Double.MAX_VALUE;
        for(var entity:level().getEntities(this,getBoundingBox().expandTowards(motion).inflate(1),e->e.isPickable()&&(e!=getOwner()||airTicks>=5))){var intercept=entity.getBoundingBox().inflate(.3).clip(rayEnd,from);if(intercept.isPresent()){double dist=rayEnd.distanceToSqr(intercept.get());if(dist<closest){target=entity;closest=dist;}}}
        boolean ignored=target instanceof Player victim&&(victim.getAbilities().invulnerable||getOwner() instanceof Player shooter&&!shooter.canHarmPlayer(victim));
        if(target!=null&&!ignored){explode();return;}
        if(target==null&&block.getType()!=HitResult.Type.MISS){motion=block.getLocation().subtract(from);Vec3 direction=motion.normalize();from=from.subtract(direction.scale(.05));setDeltaMovement(motion);inGround=true;}
        setPos(from.add(motion));setYRot((float)(Math.atan2(motion.x,motion.z)*180/Math.PI));setXRot((float)(Math.atan2(motion.y,motion.horizontalDistance())*180/Math.PI));yRotO=getYRot();xRotO=getXRot();
        if(level().isClientSide&&isInWater())for(int i=0;i<4;i++)level().addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE,getX()-motion.x*.25,getY()-motion.y*.25,getZ()-motion.z*.25,motion.x,motion.y,motion.z);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("inGround",inGround);tag.putFloat("explosionStrength",strength);tag.putInt("airTicks",airTicks);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);inGround=tag.getBoolean("inGround");strength=Math.max(0,Math.min(64,tag.getFloat("explosionStrength")));airTicks=Math.max(0,tag.getInt("airTicks"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
