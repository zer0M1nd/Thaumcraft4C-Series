package gadomancymodern.automation;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import java.util.*;

/** Direct inventory view of nearby item entities, with side-effect-free simulated operations. */
public class ArcaneDropperBlockEntity extends BlockEntity implements IItemHandler {
    private List<ItemEntity> view=List.of();private long viewTick=Long.MIN_VALUE;
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->this);
    public ArcaneDropperBlockEntity(BlockPos pos,BlockState state){super(Gadomancy.DROPPER_BE.get(),pos,state);}
    public AABB area(){Direction facing=getBlockState().getValue(ArcaneDropperBlock.FACING);Vec3 center=Vec3.atCenterOf(worldPosition.relative(facing,2));return new AABB(center,center).inflate(1.5);}
    private void refresh(){if(level!=null&&viewTick!=level.getGameTime()){viewTick=level.getGameTime();view=new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class,area(),entity->entity.isAlive()&&!entity.getItem().isEmpty()));view.sort(Comparator.comparingInt(ItemEntity::getId));}}
    private ItemEntity entity(int slot){refresh();return slot>=0&&slot<view.size()&&view.get(slot)!=null&&view.get(slot).isAlive()?view.get(slot):null;}
    @Override public int getSlots(){refresh();return view.size()+10;}
    @Override public ItemStack getStackInSlot(int slot){var entity=entity(slot);return entity==null?ItemStack.EMPTY:entity.getItem().copy();}
    @Override public int getSlotLimit(int slot){return 64;}
    @Override public boolean isItemValid(int slot,ItemStack stack){return slot>=0&&slot<getSlots();}
    @Override public ItemStack insertItem(int slot,ItemStack offered,boolean simulate){
        if(level==null||level.isClientSide||offered.isEmpty()||!isItemValid(slot,offered))return offered;
        var entity=entity(slot);ItemStack current=entity==null?ItemStack.EMPTY:entity.getItem();
        if(!current.isEmpty()&&!ItemStack.isSameItemSameTags(current,offered))return offered;
        int amount=Math.min(offered.getCount(),Math.min(64,offered.getMaxStackSize())-current.getCount());if(amount<=0)return offered;
        if(!simulate){
            if(entity!=null){var replacement=current.copy();replacement.grow(amount);entity.setItem(replacement);}
            else {
                Direction face=getBlockState().getValue(ArcaneDropperBlock.FACING);var position=Vec3.atCenterOf(worldPosition).add(face.getStepX()*.5,-.25+face.getStepY()*.5,face.getStepZ()*.5);
                var spawned=new ItemEntity(level,position.x,position.y,position.z,offered.copyWithCount(amount));spawned.setDeltaMovement(face.getStepX()*.1,face.getStepY()*.1,face.getStepZ()*.1);spawned.setPickUpDelay(3);level.addFreshEntity(spawned);
                // Preserve a newly inserted entity's slot for the remainder of this server tick.
                var expanded=new ArrayList<>(view);while(expanded.size()<=slot)expanded.add(null);expanded.set(slot,spawned);view=expanded;
            }setChanged();
        }
        return offered.copyWithCount(offered.getCount()-amount);
    }
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
        if(level==null||level.isClientSide||amount<=0)return ItemStack.EMPTY;
        var entity=entity(slot);if(entity==null||entity.hasPickUpDelay())return ItemStack.EMPTY;
        ItemStack current=entity.getItem(),removed=current.copyWithCount(Math.min(amount,current.getCount()));
        if(!simulate){ItemStack remaining=current.copy();remaining.shrink(removed.getCount());if(remaining.isEmpty())entity.discard();else entity.setItem(remaining);setChanged();}return removed;
    }
    public int comparator(){refresh();double fullness=0;boolean any=false;for(ItemEntity entity:view)if(entity!=null&&entity.isAlive()){var stack=entity.getItem();fullness+=stack.getCount()/(double)Math.min(64,stack.getMaxStackSize());any=true;}return (int)Math.floor(fullness/(view.size()+10)*14)+(any?1:0);}
    public static void tick(Level world,BlockPos pos,BlockState state,ArcaneDropperBlockEntity machine){if(world.getGameTime()%21==0)world.updateNeighbourForOutputSignal(pos,state.getBlock());}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(cap==ForgeCapabilities.ITEM_HANDLER&&(side==null||ArcaneDropperBlock.automationFace(getBlockState(),side)))return capability.cast();return super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->this);}
}
