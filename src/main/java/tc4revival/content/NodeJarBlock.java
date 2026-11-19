package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class NodeJarBlock extends BaseEntityBlock {
    public NodeJarBlock() { super(Properties.of().strength(.3f).noOcclusion().sound(new net.minecraftforge.common.util.ForgeSoundType(1,1,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR)).lightLevel(state->11)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(3,0,3,13,12,13); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new NodeJarBlockEntity(pos,state); }

    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) {
        return level.getBlockEntity(pos) instanceof NodeJarBlockEntity jar ? jar.asItem() : new ItemStack(TCItems.NODE_JAR.get());
    }
    @Override public void setPlacedBy(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof NodeJarBlockEntity jar && stack.hasTag()) jar.configure(NodeJarData.read(stack.getTag()));
    }
}
