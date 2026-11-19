package thmodern.soul;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import tc4revival.content.VisNetworkNodeBlockEntity;
import thmodern.Horizons;

public final class SoulExtractorEntity extends VisNetworkNodeBlockEntity {
    private int remaining;
    private boolean working;
    private float motion,previousMotion;
    public float sieveRotation(float partial){return previousMotion+(motion-previousMotion)*partial;}
    private ItemStack sand=ItemStack.EMPTY;
    public final ItemStackHandler input=new ItemStackHandler(1){
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(Items.SOUL_SAND);}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->new IItemHandler(){
        public int getSlots(){return 1;}public ItemStack getStackInSlot(int slot){return input.getStackInSlot(slot);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return input.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 64;}public boolean isItemValid(int slot,ItemStack stack){return input.isItemValid(slot,stack);}
    });
    public SoulExtractorEntity(BlockPos pos,BlockState state){super(Horizons.SOUL_EXTRACTOR_ENTITY.get(),pos,state);}
    @Override public int range(){return 8;}@Override public boolean source(){return false;}
    public int remaining(){return remaining;}public boolean working(){return working;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private void active(boolean value){if(working!=value){working=value;changed();}}
    public void tick(){
        tickNetwork();if(level==null)return;if(level.isClientSide){if(motion>=360)motion-=360;previousMotion=motion;if(working)motion++;return;}
        if(!sand.isEmpty()){outputSand();if(!sand.isEmpty()){active(false);return;}}
        if(!level.hasChunkAt(worldPosition.above())){active(false);return;}
        var target=level.getBlockEntity(worldPosition.above());
        boolean ready=target instanceof tc4revival.content.BrainJarBlockEntity jar?jar.insertExperience(1,true)>0
                :target instanceof SoulReceiver receiver&&receiver.soulRoom()>0;
        if(!ready){active(false);return;}
        if(remaining==0){if(input.getStackInSlot(0).isEmpty()){active(false);return;}input.extractItem(0,1,false);remaining=1200;changed();}
        if(target instanceof tc4revival.content.BrainJarBlockEntity jar){
            if(jar.insertExperience(1,true)==0){active(false);return;}
            int processed=Math.min(remaining,1+consumeVis(tc4revival.aspect.TCAspects.catalog().require("aer"),Math.min(10,remaining-1)));
            for(int i=0;i<processed;i++)if(level.random.nextInt(100)==0)jar.insertExperience(1,false);
            remaining-=processed;working=true;changed();
            if(remaining==0){sand=new ItemStack(Items.SAND);outputSand();}return;
        }
        if(!(target instanceof SoulReceiver receiver)){active(false);return;}
        int room=Math.min(remaining,receiver.soulRoom());if(room<=0){active(false);return;}
        int boost=consumeVis(tc4revival.aspect.TCAspects.catalog().require("aer"),Math.min(10,room-1));
        int accepted=receiver.acceptSoulBits(Math.min(room,1+boost));if(accepted<=0){active(false);return;}
        remaining-=Math.min(room,accepted);working=true;changed();
        if(remaining==0){sand=new ItemStack(Items.SAND);outputSand();}
    }
    private void outputSand(){
        if(level==null||level.isClientSide||sand.isEmpty())return;
        var below=worldPosition.below();if(!level.hasChunkAt(below))return;
        var target=level.getBlockEntity(below);
        if(target!=null){var handler=target.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).resolve();if(handler.isPresent())sand=ItemHandlerHelper.insertItemStacked(handler.get(),sand,false);}
        if(!sand.isEmpty()){
            var entity=new net.minecraft.world.entity.item.ItemEntity(level,worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5,sand.copy());
            if(level.addFreshEntity(entity))sand=ItemStack.EMPTY;
        }changed();
    }
    public void broken(){if(level==null||level.isClientSide)return;net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,input.extractItem(0,64,false));if(!sand.isEmpty()){net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,sand);sand=ItemStack.EMPTY;}}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return !isRemoved()&&cap==ForgeCapabilities.ITEM_HANDLER?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->new net.minecraftforge.items.wrapper.RangedWrapper(input,0,1){@Override public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}});}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Input",input.serializeNBT());tag.putInt("Remaining",remaining);tag.putBoolean("Working",working);tag.put("Sand",sand.save(new CompoundTag()));}
    @Override public void load(CompoundTag tag){super.load(tag);var data=tag.getCompound("Input").copy();data.putInt("Size",1);input.deserializeNBT(data);if(!input.getStackInSlot(0).is(Items.SOUL_SAND))input.setStackInSlot(0,ItemStack.EMPTY);else input.getStackInSlot(0).setCount(Math.min(64,input.getStackInSlot(0).getCount()));remaining=Math.max(0,Math.min(1200,tag.getInt("Remaining")));working=tag.getBoolean("Working");sand=ItemStack.of(tag.getCompound("Sand"));if(!sand.is(Items.SAND))sand=ItemStack.EMPTY;else sand.setCount(1);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
