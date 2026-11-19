package tc4revival.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

/** Arrow-based dart with the old golem aim and first-frame launch smoke. */
public  class GolemDartEntity extends AbstractArrow implements IEntityAdditionalSpawnData {
    private boolean first=true;
    public GolemDartEntity(EntityType<? extends GolemDartEntity> type,Level level) { super(type,level); pickup=Pickup.DISALLOWED; }
    public static void fire(GolemEntity golem,LivingEntity target) {
        var dart=TCEntities.GOLEM_DART.get().create(golem.level()); if (dart==null) return;
        dart.setOwner(golem);
        double y=golem.getEyeY()-(double).1f,x=target.getX()-golem.getX(),z=target.getZ()-golem.getZ();
        double height=target.getEyeY()-(double).7f-y,flat=(float)Math.sqrt(x*x+z*z);
        dart.setPos(golem.getX(),y,golem.getZ());
        if (flat>=1e-7) {
            dart.setPos(golem.getX()+x/flat/5,y,golem.getZ()+z/flat/5);
            dart.shoot(x,height+(float)flat*.2f,z,1.6f,7-golem.configuration().upgrades(3)*1.75f);
        }
        dart.setBaseDamage((float)golem.getAttributeValue(Attributes.ATTACK_DAMAGE)*.4f);
        golem.playSound(TCSounds.GOLEM_IRON_SHOOT.get(),.5f,1/(golem.getRandom().nextFloat()*.4f+.6f));
        golem.level().addFreshEntity(dart);
    }
    @Override public void shoot(double x,double y,double z,float speed,float spread) { EldritchProjectileLaunch.shoot(this,random,x,y,z,speed,spread); }
    @Override protected ItemStack getPickupItem() { return new ItemStack(Items.ARROW); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { double range=getBoundingBox().getSize()*640; return distance<range*range; }
    @Override public void tick() {
        if (first && level().isClientSide) {
            first=false; var motion=getDeltaMovement();
            for (int i=0;i<5;i++) level().addParticle(ParticleTypes.SMOKE,getX()-motion.x/1.5,getY()-motion.y/1.5,getZ()-motion.z/1.5,
                    motion.x/9+random.nextGaussian()*.01,motion.y/9+random.nextGaussian()*.01,motion.z/9+random.nextGaussian()*.01);
        }
        super.tick();
    }
    @Override public void writeSpawnData(FriendlyByteBuf data) { var motion=getDeltaMovement(); data.writeDouble(motion.x); data.writeDouble(motion.y); data.writeDouble(motion.z); data.writeFloat(getYRot()); data.writeFloat(getXRot()); }
    @Override public void readSpawnData(FriendlyByteBuf data) { setDeltaMovement(data.readDouble(),data.readDouble(),data.readDouble()); setYRot(data.readFloat()); setXRot(data.readFloat()); }
}
