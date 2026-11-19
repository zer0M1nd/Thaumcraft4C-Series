package thaumicomniverse.alignment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AlignmentPedestalEntity extends tc4revival.content.ArcanePedestalBlockEntity {
    public AlignmentPedestalEntity(BlockPos pos,BlockState state) {super(AlignmentContent.PEDESTAL_ENTITY.get(),pos,state);}
    public int sign() {return ((AlignmentPedestalBlock)getBlockState().getBlock()).sign;}
    @Override public int getMaxStackSize() {return ((AlignmentPedestalBlock)getBlockState().getBlock()).stacking?64:1;}
    @Override public void setItem(int slot,ItemStack stack) {if(slot==0)setFromInfusion(stack.copyWithCount(Math.min(stack.getCount(),Math.min(getMaxStackSize(),stack.getMaxStackSize()))));}
}
