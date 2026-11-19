package gadomancymodern.aura;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;

public class AuraCoreEntity extends ItemEntity {
    private static final EntityDataAccessor<CompoundTag> AURA=SynchedEntityData.defineId(AuraCoreEntity.class,EntityDataSerializers.COMPOUND_TAG);
    public AuraComposition composition=new AuraComposition();
    private BlockPos activation=BlockPos.ZERO;
    private java.util.List<BlockPos> scanOrder;
    private int elapsed,scannedBlocks,cursor;
    public AuraCoreEntity(EntityType<? extends AuraCoreEntity> type,Level level){super(type,level);setUnlimitedLifetime();}
    public int elapsed(){return entityData.get(AURA).getInt("elapsed");}
    public BlockPos activation(){return BlockPos.of(entityData.get(AURA).getLong("activation"));}
    public java.util.List<String> orbitals(){var tag=entityData.get(AURA);var result=new AuraComposition();for(String a:AuraComposition.PRIMALS)result.add(a,tag.getInt(a));return result.orbitals();}
    public void activate(ItemEntity dropped,BlockPos crystal,java.util.List<String> primals){
        setPos(dropped.position());setDeltaMovement(dropped.getDeltaMovement());setItem(dropped.getItem().copy());activation=crystal.immutable();
        primals.forEach(a->composition.add(a,primals.size()==1?60:10));initializeOrder();synchronize();
    }
    private void initializeOrder(){scanOrder=new java.util.ArrayList<>(729);for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)scanOrder.add(new BlockPos(x,y,z));java.util.Collections.shuffle(scanOrder,new java.util.Random(activation.asLong()));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(AURA,new CompoundTag());}
    @Override public void tick(){
        super.tick();if(!isAlive())return;
        if(level().isClientSide){gadomancymodern.client.AuraCoreEffects.tick(this);return;}
        elapsed++;if(scanOrder==null)initializeOrder();
        int desired=AuraComposition.consumedAt(elapsed);while(cursor<desired&&cursor<scanOrder.size())sample(scanOrder.get(cursor++));
        synchronize();if(elapsed>500)finish();
    }
    private void sample(BlockPos offset){
        // Original rounds world-space sums toward zero, including negative coordinates.
        var pos=new BlockPos((int)(getX()+offset.getX()),(int)(getY()+offset.getY()),(int)(getZ()+offset.getZ()));
        if(!level().hasChunkAt(pos))return;var state=level().getBlockState(pos);if(state.isAir())return;
        AspectAmounts aspects=BlockScanTargets.target(state).map(BlockScanTargets::aspects).orElseGet(()->ItemStackAspects.resolve(new ItemStack(state.getBlock())).orElseGet(AspectAmounts::new));
        if(aspects.isEmpty())return;scannedBlocks++;for(var entry:aspects.view().entrySet())reduce(entry.getKey(),entry.getValue());
    }
    private void reduce(Aspect aspect,int amount){
        var values=TCAspects.catalog().toPrimals(new AspectAmounts().add(aspect,amount),AspectCatalog.Reduction.SUM);
        values.view().forEach((a,n)->{if(thaumcraft.api.aspects.AspectSelection.includes(thaumcraft.api.aspects.AspectSelection.NODE_MANIPULATION,a))composition.add(a.tag(),n);});
    }
    private void synchronize(){var tag=new CompoundTag();tag.putInt("elapsed",elapsed);tag.putLong("activation",activation.asLong());for(String a:AuraComposition.PRIMALS)tag.putInt(a,composition.amount(a));entityData.set(AURA,tag);}
    public void finish(){
        if(level().isClientSide)return;String type=composition.choose(scannedBlocks,random::nextInt);
        var result=new ItemEntity(level(),getX(),getY(),getZ(),new ItemStack(Gadomancy.AURA_CORES.get(type).get()));result.setDeltaMovement(0,0,0);level().addFreshEntity(result);
        // Entity events use the vanilla tracked-entity channel; no unregistered particle ids.
        level().broadcastEntityEvent(this,(byte)(type.equals("blank")?61:62));discard();
    }
    @Override public void handleEntityEvent(byte event){if(event==61||event==62)gadomancymodern.client.AuraCoreEffects.finish(this,event==62);else super.handleEntityEvent(event);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("GatherElapsed",elapsed);tag.putInt("GatherCursor",cursor);tag.putInt("GatherBlocks",scannedBlocks);tag.putLong("Activation",activation.asLong());var weights=new CompoundTag();for(String a:AuraComposition.PRIMALS)weights.putInt(a,composition.amount(a));tag.put("AuraWeights",weights);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);elapsed=Math.max(0,tag.getInt("GatherElapsed"));cursor=Math.max(0,Math.min(729,tag.getInt("GatherCursor")));scannedBlocks=Math.max(0,tag.getInt("GatherBlocks"));activation=BlockPos.of(tag.getLong("Activation"));composition=new AuraComposition();for(String a:AuraComposition.PRIMALS)composition.add(a,tag.getCompound("AuraWeights").getInt(a));initializeOrder();synchronize();}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
