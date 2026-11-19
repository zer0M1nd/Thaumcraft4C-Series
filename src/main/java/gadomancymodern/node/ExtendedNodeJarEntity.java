package gadomancymodern.node;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
public class ExtendedNodeJarEntity extends NodeJarBlockEntity {
    public CompoundTag extension=new CompoundTag();
    public ExtendedNodeJarEntity(BlockPos pos,BlockState state){super(Gadomancy.EXTENDED_JAR_BE.get(),pos,state);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Gadomancy",extension.copy());}
    @Override public void load(CompoundTag tag){super.load(tag);extension=tag.getCompound("Gadomancy").copy();}
    @Override public ItemStack asItem(){var stack=new ItemStack(Gadomancy.EXTENDED_JAR_ITEM.get());var tag=contents().write();tag.put("Gadomancy",extension.copy());stack.setTag(tag);return stack;}
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context){var extra=extension.copy();var result=super.onWandUseFirst(wand,context);if(result.consumesAction()&&level instanceof net.minecraft.server.level.ServerLevel server&&server.getBlockEntity(worldPosition) instanceof AuraNodeBlockEntity){var node=ExtendedNodeEntity.convert(server,worldPosition);if(node!=null){node.extension=extra.getString("Type");node.growth.load(extra.getCompound("Growth"));node.sync();}}return result;}
}
