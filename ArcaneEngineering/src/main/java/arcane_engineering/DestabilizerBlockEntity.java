package arcane_engineering;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.*;
import tc4revival.content.*;
import thaumcraft.api.nodes.NodeApi;
import thaumcraft.api.aspects.*;

public class DestabilizerBlockEntity extends BlockEntity {
    public static final int CAPACITY=100000, RUN_COST=100;
    private int energy;
    public boolean open;
    public float angle, previousAngle, speed;
    private LazyOptional<IEnergyStorage> capability=LazyOptional.empty();
    private final IEnergyStorage storage=new IEnergyStorage() {
        @Override public int receiveEnergy(int requested,boolean simulate) {
            if(level==null || level.isClientSide || requested<=0) return 0;
            int accepted=Math.min(requested,CAPACITY-energy);
            if(!simulate && accepted>0) { energy+=accepted; setChanged(); }
            return accepted;
        }
        @Override public int extractEnergy(int amount,boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy; }
        @Override public int getMaxEnergyStored() { return CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };
    public DestabilizerBlockEntity(BlockPos pos,BlockState state) { super(ArcaneEngineering.DESTABILIZER_TILE.get(),pos,state); capability=LazyOptional.of(()->storage); }
    public int energy() { return energy; }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) { return cap==ForgeCapabilities.ENERGY && side==Direction.DOWN ? capability.cast() : super.getCapability(cap,side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); capability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); capability=LazyOptional.of(()->storage); }
    public static void tick(Level level,BlockPos pos,BlockState state,DestabilizerBlockEntity device) {
        if(!level.isClientSide) {
            boolean wasOpen=device.open;
            var above=level.getBlockEntity(pos.above());
            if(!device.open) {
                if(device.energy>=RUN_COST) device.speed=(float)(Math.pow((double)device.energy/CAPACITY,2.5)*24);
                else if(device.speed>1.5f) device.speed=Math.max(0,device.speed-.1f);
                else if(device.angle%90<=1.5f) { device.speed=0; device.angle=0; }
                if(device.energy==CAPACITY && above instanceof AuraNodeBlockEntity node) {
                    NodeApi.edit(node,NodeApi.getBase(node),AuraNodeType.HUNGRY,node.nodeModifier());
                    device.open=true; device.energy=0;
                    level.playSound(null,pos,TCSounds.RUNIC_SHIELD_CHARGE.get(),SoundSource.BLOCKS,1,1);
                }
            } else if(above instanceof AuraNodeBlockEntity node) {
                if(device.energy>=RUN_COST) device.energy-=RUN_COST;
                else {
                    device.open=false;
                    if(level.random.nextFloat()<.75f) {
                        NodeApi.edit(node,NodeApi.getBase(node),AuraNodeType.NORMAL,node.nodeModifier());
                        level.playSound(null,pos,TCSounds.CRAFT_FAIL.get(),SoundSource.BLOCKS,1,1);
                    } else {
                        NodeApi.configure(node,new AspectList().add(Aspect.TAINT,1),AuraNodeType.TAINTED,AuraNodeModifier.FADING,false);
                        level.explode(null,pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,2,Level.ExplosionInteraction.BLOCK);
                    }
                }
            } else device.open=false;
            if(device.open || wasOpen || device.speed>0) device.setChanged();
            if(wasOpen!=device.open || level.getGameTime()%10==0) level.sendBlockUpdated(pos,state,state,3);
        }
        device.previousAngle=device.angle;
        device.angle=(device.angle+device.speed)%360;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Energy",energy); tag.putBoolean("open",open); tag.putFloat("speed",speed); tag.putFloat("angle",angle); }
    @Override public void load(CompoundTag tag) { super.load(tag); energy=Math.max(0,Math.min(CAPACITY,tag.getInt("Energy"))); open=tag.getBoolean("open"); speed=Math.max(0,Math.min(24,tag.getFloat("speed"))); angle=tag.getFloat("angle")%360; previousAngle=angle; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).inflate(.25); }
}
