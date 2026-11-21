package gadomancymodern.knowledge;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** The item entity owns the note. The book stores only its observable research state. */
public class HeldResearchEntity extends tc4revival.content.PermanentItemEntity {
    private static final EntityDataAccessor<BlockPos> MASTER=SynchedEntityData.defineId(HeldResearchEntity.class,EntityDataSerializers.BLOCK_POS);
    public HeldResearchEntity(EntityType<? extends ItemEntity> type,Level level){super(type,level);}
    public HeldResearchEntity(Level level,BlockPos master,ItemStack stack){this(Gadomancy.HELD_RESEARCH.get(),level);entityData.set(MASTER,master.immutable());setPos(master.getX()+.5,master.getY()+.7,master.getZ()+.5);setItem(stack);setPickUpDelay(60);setDeltaMovement(Vec3.ZERO);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(MASTER,BlockPos.ZERO);}
    @Override public void tick(){
        noPhysics=true;super.tick();if(!isAlive())return;setDeltaMovement(Vec3.ZERO);var pos=entityData.get(MASTER);setPos(pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5);
        if(!level().isClientSide&&tickCount%2==0&&level().getBlockEntity(pos) instanceof KnowledgeBookEntity book)book.heartbeat(this);
        if(!level().isClientSide&&tickCount%8==0&&!(level().getBlockEntity(pos) instanceof KnowledgeBookEntity)){
            // Transfer ownership once; a missing master never deletes or duplicates the note.
            var item=new ItemEntity(level(),getX(),getY(),getZ(),getItem().copy());if(level().addFreshEntity(item)){setItem(ItemStack.EMPTY);discard();}
        }
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){if(source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)||source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT))return false;return super.hurt(source,amount);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putLong("GadMaster",entityData.get(MASTER).asLong());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(MASTER,BlockPos.of(tag.getLong("GadMaster")));}
}
