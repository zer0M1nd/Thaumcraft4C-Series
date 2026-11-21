package thmodern.soul;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import thmodern.Horizons;

public final class InspiratronEntity extends BlockEntity implements SoulReceiver {
    private int progress;
    private float rotation,previousRotation;
    public float brainRotation(float partial){return previousRotation+net.minecraft.util.Mth.wrapDegrees(rotation-previousRotation)*partial;}
    public void tick(){if(level==null||!level.isClientSide)return;previousRotation=rotation;var player=level.getNearestPlayer(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,6,false);float target=player==null?rotation+1:(float)Math.toDegrees(Math.atan2(player.getZ()-worldPosition.getZ()-.5,player.getX()-worldPosition.getX()-.5));rotation=net.minecraft.util.Mth.wrapDegrees(rotation+net.minecraft.util.Mth.wrapDegrees(target-rotation)*.04f);}
    public final ItemStackHandler inventory=new ItemStackHandler(2){
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&stack.is(Items.PAPER);}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    private LazyOptional<IItemHandler> capability=LazyOptional.of(this::automated);
    public InspiratronEntity(BlockPos pos,BlockState state){super(Horizons.INSPIRATRON_ENTITY.get(),pos,state);}
    public static Item fragment(){return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:knowledge_fragment"));}
    public int progress(){return progress;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int soulRoom(){int capacity=Math.min(inventory.getStackInSlot(0).is(Items.PAPER)?inventory.getStackInSlot(0).getCount():0,64-inventory.getStackInSlot(1).getCount());return capacity>0?capacity*100-progress:0;}
    @Override public int acceptSoulBits(int amount){
        if(level==null||level.isClientSide||amount<=0)return 0;int accepted=Math.min(amount,soulRoom());if(accepted==0)return 0;
        for(int i=0;i<accepted;i++)if(level.random.nextDouble()>=.97){
            progress++;
            if(progress>=100){progress-=100;inventory.extractItem(0,1,false);var output=inventory.getStackInSlot(1);inventory.setStackInSlot(1,new ItemStack(fragment(),output.getCount()+1));}
        }
        changed();return accepted;
    }
    private IItemHandler automated(){return new RangedWrapper(inventory,0,2){@Override public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==1?super.extractItem(slot,amount,simulate):ItemStack.EMPTY;}};}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return !isRemoved()&&cap==ForgeCapabilities.ITEM_HANDLER?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(this::automated);}
    public void broken(){if(level!=null&&!level.isClientSide)for(int slot=0;slot<2;slot++)net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,inventory.extractItem(slot,64,false));}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Progress",progress);tag.put("Inventory",inventory.serializeNBT());}
    @Override public void load(CompoundTag tag){super.load(tag);progress=Math.max(0,Math.min(99,tag.getInt("Progress")));var data=tag.getCompound("Inventory").copy();data.putInt("Size",2);inventory.deserializeNBT(data);for(int i=0;i<2;i++){var item=inventory.getStackInSlot(i);if(!item.is(i==0?Items.PAPER:fragment()))inventory.setStackInSlot(i,ItemStack.EMPTY);else item.setCount(Math.min(64,item.getCount()));}}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
