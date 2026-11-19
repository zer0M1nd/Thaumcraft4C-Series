package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.items.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

public final class MagnetEntity extends BlockEntity {
    private boolean adult=true;
    public final ItemStackHandler filter=new ItemStackHandler(1){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return mob()&&stack.is(Tinkerer.SOUL_MOULD.get());}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->filter);
    public MagnetEntity(BlockPos pos,BlockState state){super(Tinkerer.MAGNET_ENTITY.get(),pos,state);}
    public boolean mob(){return getBlockState().getBlock() instanceof MagnetBlock block&&block.mob;}
    public boolean adult(){return adult;}
    public void adult(boolean value){adult=value;changed();}
    public int signal(){return level==null?0:level.getBestNeighborSignal(worldPosition);}
    public boolean pulling(){return getBlockState().getValue(MagnetBlock.PULLING);}
    public void pulling(boolean value){if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(MagnetBlock.PULLING,value),3);}
    public boolean accepts(Entity entity){
        if(!entity.isAlive())return false;
        if(!mob())return entity instanceof ItemEntity;
        if(!(entity instanceof LivingEntity)||entity instanceof Player)return false;
        if(entity instanceof AgeableMob ageable&&adult==ageable.isBaby())return false;
        var mould=filter.getStackInSlot(0);if(mould.isEmpty())return true;
        return SoulMouldItem.pattern(mould).equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());
    }
    public void tick(){
        if(level==null||level.isClientSide)return;int power=signal();if(power==0)return;
        double range=power/2d; // The original manual specifies 7.5 blocks at signal 15.
        var center=worldPosition.getCenter();var box=new AABB(center.x-range,worldPosition.getY(),center.z-range,center.x+range,center.y+range,center.z+range);
        for(var entity:level.getEntities((Entity)null,box,this::accepts)){
            if(pulling()&&entity.position().distanceToSqr(center)<=1)continue;
            TinkererFocus.pull(entity,center,pulling()?.25:-.25);
            if(level instanceof net.minecraft.server.level.ServerLevel server)TinkererFocus.dust(server,entity.position(),pulling()?0x3333ff:0xff3333,1,0);
        }
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean usable(Player player){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Adult",adult);tag.put("Filter",filter.serializeNBT());}
    @Override public void load(CompoundTag tag){super.load(tag);adult=!tag.contains("Adult")||tag.getBoolean("Adult");filter.deserializeNBT(tag.getCompound("Filter"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&mob()&&!isRemoved()?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->filter);}
}
