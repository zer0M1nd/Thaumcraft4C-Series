package gadomancymodern.node;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class ExtendedNodeJarBlock extends tc4revival.content.NodeJarBlock {
    @Override public String getDescriptionId(){return tc4revival.content.TCArcane.NODE_JAR.get().getDescriptionId();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ExtendedNodeJarEntity(pos,state);}
    @Override public void setPlacedBy(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(level.getBlockEntity(pos) instanceof ExtendedNodeJarEntity jar&&stack.hasTag())jar.extension=stack.getTag().getCompound("Gadomancy").copy();}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){var tile=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);return tile instanceof ExtendedNodeJarEntity jar&&jar.drop?java.util.List.of(jar.asItem()):java.util.List.of();}
}
