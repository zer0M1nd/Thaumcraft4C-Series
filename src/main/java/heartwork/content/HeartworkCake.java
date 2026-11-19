package heartwork.content;

import heartwork.Heartwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

public class HeartworkCake extends CakeBlock {
    private final String item;
    public HeartworkCake(String item) { super(BlockBehaviour.Properties.copy(Blocks.CAKE)); this.item = item; }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(Heartwork.item(item)); }
}
