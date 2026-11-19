package tmmodern;
import java.util.UUID;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.entity.*;import net.minecraft.world.entity.projectile.*;import net.minecraft.world.level.Level;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.phys.*;import net.minecraft.nbt.CompoundTag;
public class HomingShardProjectile extends ThrowableProjectile implements net.minecraftforge.entity.IEntityAdditionalSpawnData {
    public float strength;public boolean persistent;public UUID target;public ResourceLocation targetType;
    public HomingShardProjectile(EntityType<? extends HomingShardProjectile> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(){}@Override protected float getGravity(){return 0;}
    @Override public void tick(){
        super.tick();
        if(level() instanceof ServerLevel server){
            Entity selected=target==null?null:server.getEntity(target);
            // The original only uses the range threshold to look for a replacement when
            // Persistent is installed. An ordinary shard keeps following a living target.
            if(persistent&&(selected==null||!selected.isAlive()||distanceToSqr(selected)>1250)){
                var replacement=level().getEntities(this,getBoundingBox().inflate(16),
                    e->e instanceof LivingEntity&&e!=getOwner()&&e.isAlive()
                        &&(targetType==null||targetType.equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()))))
                    .stream().findFirst().orElse(null);
                if(replacement!=null){selected=replacement;target=replacement.getUUID();}
            }
            if(selected==null||!selected.isAlive()||tickCount>300){
                tc4revival.network.FocusEffectPacket.burst(level(),position(),.3f);discard();return;
            }
            if(tickCount%10==0){
                double distance=distanceTo(selected);
                if(distance>.0001)setDeltaMovement(selected.position().add(0,selected.getBbHeight()*.6,0).subtract(position()).scale(1/distance));
                TMRuntime.sound(level(),blockPosition(),"taintedmagic:shard",.1f,2*random.nextFloat());
            }
        }
        setDeltaMovement(getDeltaMovement().scale(.85));
        if(level().isClientSide)TMEffects.localSparkles(level(),position(),1,0,.5f,0);
    }
    @Override protected void onHit(HitResult hit){if(hit instanceof BlockHitResult blockHit){var state=level().getBlockState(blockHit.getBlockPos());if(state.is(net.minecraft.tags.BlockTags.LEAVES)||state.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES)||state.is(net.minecraft.world.level.block.Blocks.COBWEB)||state.is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL))return;setDeltaMovement(getDeltaMovement().scale(-.8));return;}if(hit instanceof EntityHitResult e&&e.getEntity()!=getOwner()&&!level().isClientSide){e.getEntity().hurt(damageSources().indirectMagic(this,getOwner()),2+.5f*strength);TMRuntime.sound(level(),blockPosition(),"thaumcraft:zap",1,1+(random.nextFloat()-random.nextFloat())*.2f);tc4revival.network.FocusEffectPacket.burst(level(),position(),.3f);discard();}}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void writeSpawnData(net.minecraft.network.FriendlyByteBuf b){b.writeFloat(strength);}
    @Override public void readSpawnData(net.minecraft.network.FriendlyByteBuf b){strength=b.readFloat();}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putFloat("Strength",strength);t.putBoolean("Persistent",persistent);if(target!=null)t.putUUID("Target",target);if(targetType!=null)t.putString("TargetType",targetType.toString());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);strength=t.getFloat("Strength");persistent=t.getBoolean("Persistent");target=t.hasUUID("Target")?t.getUUID("Target"):null;targetType=ResourceLocation.tryParse(t.getString("TargetType"));}
}
