package gadomancymodern.aura;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
public class HeldEssenceEntity extends tc4revival.content.PermanentItemEntity {
    protected static final EntityDataAccessor<BlockPos> MASTER=SynchedEntityData.defineId(HeldEssenceEntity.class,EntityDataSerializers.BLOCK_POS);
    public HeldEssenceEntity(EntityType<? extends ItemEntity> type,Level level){super(type,level);}
    public HeldEssenceEntity(Level level,BlockPos master,ItemStack stack){this(Gadomancy.HELD_ESSENCE.get(),level);entityData.set(MASTER,master.immutable());setPos(master.getX()+.5,master.getY()-.2,master.getZ()+.5);setItem(stack);setPickUpDelay(60);setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(MASTER,BlockPos.ZERO);}
    @Override public void tick(){noPhysics=true;super.tick();if(!isAlive())return;setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);var master=entityData.get(MASTER);setPos(master.getX()+.5,master.getY()-.2,master.getZ()+.5);if(level().isClientSide)return;if(level().getBlockEntity(master) instanceof AuraPylonEntity pylon&&pylon.formed&&pylon.index==2){if(tickCount%2==0)pylon.heartbeat(this);}else if(tickCount%8==0){var released=new ItemEntity(level(),getX(),getY(),getZ(),getItem().copy());if(level().addFreshEntity(released)){setItem(ItemStack.EMPTY);discard();}}}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putLong("Pylon",entityData.get(MASTER).asLong());}@Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(MASTER,BlockPos.of(tag.getLong("Pylon")));}
}
