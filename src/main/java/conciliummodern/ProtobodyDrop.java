package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Saves each accepted part independently, so a cancelled spawn or reload cannot duplicate limbs. */
public class ProtobodyDrop extends ItemEntity {
    private int spawnedMask,retry;
    public ProtobodyDrop(EntityType<? extends ProtobodyDrop> type,Level level){super(type,level);}
    @Override public void tick(){
        if(!level().isClientSide && getY()<level().getMinBuildHeight()-16 && net.minecraftforge.fml.ModList.get().isLoaded("taintedmagic") && !getItem().isEmpty()){
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);setNoGravity(true);
            if(retry--<=0){retry=20;
                for(int type=0;type<6;type++)if((spawnedMask&(1<<type))==0){
                    var limb=ConciliumEntities.PROTOLIMB.get().create(level());if(limb==null)continue;
                    limb.moveTo(getX(),getY(),getZ(),getYRot(),getXRot());limb.setLimbType(type);
                    var motion=new net.minecraft.world.phys.Vec3((random.nextDouble()-.5)*2,1,(random.nextDouble()-.5)*2).normalize().scale(.5);limb.setDeltaMovement(motion);
                    if(level().addFreshEntity(limb))spawnedMask|=1<<type;
                }
                if(spawnedMask==63){var remaining=getItem().copy();remaining.shrink(1);if(remaining.isEmpty()){discard();return;}setItem(remaining);spawnedMask=0;}
            }
        }else setNoGravity(false);
        super.tick();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ConciliumParts",spawnedMask);tag.putInt("ConciliumRetry",retry);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);spawnedMask=tag.getInt("ConciliumParts")&63;retry=Math.max(0,Math.min(20,tag.getInt("ConciliumRetry")));}
}
