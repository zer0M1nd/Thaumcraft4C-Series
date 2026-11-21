package automagy.content;

import automagy.api.LocationLink;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ScribePointerBlockEntity extends BlockEntity {
    private final List<ItemStack> eyes=new ArrayList<>();
    public ScribePointerBlockEntity(BlockPos pos,BlockState state){super(Content.SCRIBE_POINTER_TILE.get(),pos,state);}
    private Optional<BlockPos> target(ItemStack stack){
        if(level==null||!(stack.getItem() instanceof LocationLink link))return Optional.empty();
        return link.linkedPosition(stack).filter(p->p.dimension().equals(level.dimension())&&!p.pos().equals(worldPosition)&&p.pos().distSqr(worldPosition)<=100).map(GlobalPos::pos);
    }
    public boolean insert(ItemStack stack){
        if(eyes.size()>=5||target(stack).isEmpty())return false;
        eyes.add(stack.copyWithCount(1));setChanged();return true;
    }
    public ItemStack remove(){if(eyes.isEmpty())return ItemStack.EMPTY;var result=eyes.remove(eyes.size()-1);setChanged();return result;}
    public int rangeBoosters(){int count=0;for(ItemStack eye:eyes)if(target(eye).filter(worldPosition.below()::equals).isPresent())count++;return count;}
    public Set<Block> selectedBlocks(){
        Set<Block> selected=new LinkedHashSet<>();
        for(ItemStack eye:eyes)target(eye).filter(pos->!pos.equals(worldPosition.below())&&level.hasChunkAt(pos)).ifPresent(pos->{
            if(InventariumContents.inventory(level,new InventariumContents.Source(pos,null)).isPresent())selected.add(level.getBlockState(pos).getBlock());
        });
        return selected;
    }
    public void drop(){if(level!=null&&!level.isClientSide){for(ItemStack eye:eyes)Block.popResource(level,worldPosition,eye);eyes.clear();}}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ListTag list=new ListTag();for(ItemStack eye:eyes)list.add(eye.save(new CompoundTag()));tag.put("Eyes",list);}
    @Override public void load(CompoundTag tag){super.load(tag);eyes.clear();ListTag list=tag.getList("Eyes",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(5,list.size());i++){ItemStack eye=ItemStack.of(list.getCompound(i));if(!eye.isEmpty()&&eye.getItem() instanceof LocationLink)eyes.add(eye.copyWithCount(1));}}
}
