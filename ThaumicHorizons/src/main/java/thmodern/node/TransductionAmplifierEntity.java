package thmodern.node;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import thaumcraft.api.nodes.EnergizedNodeApi;
import thaumcraft.api.aspects.AspectList;
import thmodern.Horizons;

public final class TransductionAmplifierEntity extends BlockEntity {
    private UUID device=UUID.randomUUID();
    private int activeTicks;
    private boolean active;
    public TransductionAmplifierEntity(BlockPos pos,BlockState state){super(Horizons.TRANSDUCTION_AMPLIFIER_ENTITY.get(),pos,state);}
    public boolean active(){return active;}
    public int extension(){return Math.min(50,activeTicks);}
    public BlockPos target(){return worldPosition.relative(getBlockState().getValue(TransductionAmplifierBlock.FACING).getOpposite());}
    private EnergizedNodeBlockEntity node(){return level!=null&&level.hasChunkAt(target())&&level.getBlockEntity(target()) instanceof EnergizedNodeBlockEntity n?n:null;}
    public void revoke(){var n=node();if(n!=null)EnergizedNodeApi.remove(n,device);active=false;activeTicks=0;}
    private void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    public void tick(){
        if(level==null||level.isClientSide)return;
        if(!level.hasChunkAt(target()))return;
        var n=node();
        if(n==null){revoke();level.destroyBlock(worldPosition,true);return;}
        if(level.dimension().location().equals(thmodern.plane.PocketPlanes.DIMENSION)){
            revoke();level.destroyBlock(worldPosition,true);level.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,1,net.minecraft.world.level.Level.ExplosionInteraction.NONE);return;
        }
        if(!level.hasNeighborSignal(worldPosition)){if(active){revoke();sync();}return;}
        var bonus=new tc4revival.aspect.AspectAmounts();
        for(var aspect:EnergizedNodeApi.permanentAura(n).unwrap().aspects())if(aspect.isPrimal())bonus.add(aspect,10);
        if(bonus.isEmpty()){if(active){revoke();sync();}return;}
        boolean previous=active;active=EnergizedNodeApi.amplify(n,device,AspectList.from(bonus),3);
        if(active)activeTicks++;
        if(previous!=active||activeTicks<=50||activeTicks%50==0)sync();
        if(!active||activeTicks%50!=0)return;
        int count=EnergizedNodeApi.activeAmplifiers(n);
        if(count>1)for(var target:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(worldPosition).inflate(10))){
            target.hurt(level.damageSources().magic(),4);spark(target.getBoundingBox().getCenter());
        }
        if(count>3){
            var base=EnergizedNodeApi.permanentAura(n).unwrap();var pos=target();
            EnergizedNodeApi.clear(n);
            // The rift eats surrounding machinery. Remove its converter before notifying
            // neighbours so the base converter cannot collapse the new rift as a lost node.
            if(level.getBlockEntity(pos.above()) instanceof NodeConverterBlockEntity){
                var state=level.getBlockState(pos.above());level.setBlock(pos.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
                net.minecraft.world.level.block.Block.dropResources(state,level,pos.above());
            }
            level.setBlock(pos,Horizons.VORTEX.get().defaultBlockState(),2);
            if(level.getBlockEntity(pos) instanceof thmodern.plane.VortexEntity vortex)vortex.initialize(base,false);
            level.updateNeighborsAt(pos,Horizons.VORTEX.get());return;
        }
        if(count>2&&level.random.nextInt(4)==2){
            var pos=worldPosition.offset(level.random.nextInt(16)-8,level.random.nextInt(16)-8,level.random.nextInt(16)-8);
            if(level.hasChunkAt(pos)&&level.isEmptyBlock(pos)){
                level.setBlock(pos,(pos.getY()>worldPosition.getY()?TCArcane.FLUX_GAS.get():TCArcane.FLUX_GOO.get()).defaultBlockState(),3);spark(pos.getCenter());
            }
        }
    }
    private void spark(net.minecraft.world.phys.Vec3 end){
        tc4revival.network.FocusEffectPacket.lightning(level,worldPosition.getCenter(),end);
    }
    @Override public void setRemoved(){revoke();super.setRemoved();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putUUID("Device",device);tag.putInt("ActiveTicks",activeTicks);tag.putBoolean("Active",active);}
    @Override public void load(CompoundTag tag){super.load(tag);if(tag.hasUUID("Device"))device=tag.getUUID("Device");activeTicks=Math.max(0,tag.getInt("ActiveTicks"));active=tag.getBoolean("Active");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
