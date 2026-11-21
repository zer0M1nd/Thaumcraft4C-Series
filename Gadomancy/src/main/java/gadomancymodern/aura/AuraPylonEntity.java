package gadomancymodern.aura;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Three conduit sections and one cap. The bottom owns essentia, the upper conduit owns the crystal. */
public class AuraPylonEntity extends BlockEntity implements thaumcraft.api.aspects.IEssentiaTransport,RevealedAspectContainer {
    public boolean formed;public int index=-1,amount,ticks,heartbeat;public String aspect="";public ItemStack observedCrystal=ItemStack.EMPTY;
    public static java.util.function.Consumer<AuraPylonEntity> CLIENT_EFFECTS=tile->{};
    public AuraPylonEntity(BlockPos pos,BlockState state){super(Gadomancy.PYLON_BE.get(),pos,state);}
    public boolean cap(){return getBlockState().is(Gadomancy.PYLON_CAP.get());}
    public AuraPylonEntity section(int requested){if(level==null||!formed)return null;var at=worldPosition.offset(0,requested-index,0);return level.getBlockEntity(at) instanceof AuraPylonEntity pylon&&pylon.formed&&pylon.index==requested?pylon:null;}
    public static void tick(Level level,BlockPos pos,BlockState state,AuraPylonEntity tile){tile.ticks++;tile.heartbeat++;if(level.isClientSide){CLIENT_EFFECTS.accept(tile);return;}
        if(tile.cap()&&!(level.getBlockEntity(pos.below()) instanceof AuraPylonEntity below&&!below.cap())){level.destroyBlock(pos,true);return;}
        if(!tile.formed)return;if(tile.ticks%4==0&&!tile.valid()){level.destroyBlock(pos,true);return;}
        if(tile.index==0&&tile.ticks%16==0&&tile.amount<15&&!tile.aspect.isEmpty()&&level.getBlockEntity(pos.below()) instanceof EssentiaTransport source&&source.connectable(Direction.UP)&&source.canOutput(Direction.UP)&&source.suction(Direction.UP)<64){var type=TCAspects.catalog().find(tile.aspect).orElse(null);if(type!=null)tile.addEssentia(type,source.takeEssentia(type,1,Direction.UP),Direction.DOWN);}
        if(tile.index==2){var input=tile.section(0);if(input!=null&&input.amount>0){if(tile.ticks%32==0){input.amount--;input.changed();}AuraEffectRegistry.distribute(tile.aspect,(net.minecraft.server.level.ServerLevel)level,pos,tile.ticks);}if(!tile.aspect.isEmpty()&&tile.heartbeat>8)tile.clearCrystal();if(tile.observedCrystal.isEmpty())tile.attract();}
    }
    public boolean valid(){for(int i=0;i<4;i++){var section=section(i);if(section==null||section.cap()!=(i==3))return false;}return true;}
    public void heartbeat(HeldEssenceEntity item){if(!formed||index!=2)return;heartbeat=0;var held=item.getItem();if(held.getItem() instanceof WispEssenceItem essence){var values=essence.storedAspects(held);if(!values.isEmpty()){String tag=values.aspects().get(0).tag();if(!ItemStack.isSameItemSameTags(held,observedCrystal)||!tag.equals(aspect)){observedCrystal=held.copyWithCount(1);setAspect(tag);}}}}
    public Vec3 crystalPosition(){return new Vec3(worldPosition.getX()+.5,worldPosition.getY()-.2,worldPosition.getZ()+.5);}
    protected void attract(){var center=crystalPosition().add(0,.2,0);var found=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center.subtract(.5,.5,.5),center.add(.5,.5,.5)).inflate(8),item->item.isAlive()&&!(item instanceof PermanentItemEntity)&&item.getItem().getItem() instanceof WispEssenceItem essence&&!essence.storedAspects(item.getItem()).isEmpty());var item=found.stream().min(java.util.Comparator.comparingDouble(entity->entity.position().distanceToSqr(center))).orElse(null);if(item==null)return;
        if(item.position().distanceToSqr(center)<1){var held=new HeldEssenceEntity(level,worldPosition,item.getItem().copyWithCount(1));if(level.addFreshEntity(held)){item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();item.noPhysics=false;heartbeat(held);}}
        else{item.noPhysics=true;var delta=worldPosition.getCenter().subtract(item.position()).scale(1.0/15);double distance=delta.length(),factor=1-distance;if(distance>1e-7&&factor>0){factor*=factor;item.setDeltaMovement(item.getDeltaMovement().add(delta.x/distance*factor*.15,delta.y/distance*factor*.25,delta.z/distance*factor*.15));item.hasImpulse=true;}}
    }
    protected void setAspect(String selected){for(int i=0;i<4;i++){var section=section(i);if(section!=null){section.aspect=selected;section.changed();}}}
    public void clearCrystal(){observedCrystal=ItemStack.EMPTY;setAspect("");var input=section(0);if(input!=null){input.amount=0;input.changed();}}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public AspectAmounts revealedAspects(){var input=section(0);var result=new AspectAmounts();if(input!=null&&input.amount>0)TCAspects.catalog().find(input.aspect).ifPresent(a->result.add(a,input.amount));return result;}
    @Override public boolean connectable(Direction face){return formed&&index==0&&face==Direction.DOWN;}
    @Override public boolean canInput(Direction face){return connectable(face);}@Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){return formed&&index==0?TCAspects.catalog().find(aspect).orElse(null):null;}
    @Override public int suction(Direction face){return suctionType(face)==null?0:64;}@Override public int minimumSuction(){return 64;}
    @Override public Aspect essentiaType(Direction face){return suctionType(face);}@Override public int essentiaAmount(Direction face){return formed&&index==0?amount:0;}
    @Override public int takeEssentia(Aspect type,int requested,Direction face){return 0;}
    @Override public int addEssentia(Aspect type,int requested,Direction face){if(!canInput(face)||type==null||!type.tag().equals(aspect)||requested<=0)return 0;int accepted=Math.min(requested,15-amount);amount+=accepted;if(accepted>0)changed();return accepted;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Formed",formed);tag.putInt("Index",index);tag.putInt("Amount",amount);tag.putString("Aspect",aspect);if(!observedCrystal.isEmpty())tag.put("Crystal",observedCrystal.save(new CompoundTag()));}
    @Override public void load(CompoundTag tag){super.load(tag);formed=tag.getBoolean("Formed");index=tag.getInt("Index");amount=Math.max(0,Math.min(15,tag.getInt("Amount")));aspect=tag.getString("Aspect");observedCrystal=ItemStack.of(tag.getCompound("Crystal"));heartbeat=0;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}@Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
