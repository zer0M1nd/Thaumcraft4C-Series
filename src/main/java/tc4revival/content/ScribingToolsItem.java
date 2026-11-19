package tc4revival.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public  class ScribingToolsItem extends Item implements thaumcraft.api.IScribeTools {
    public ScribingToolsItem() { super(new Properties().durability(100)); }
    public static boolean accepts(ItemStack stack) { return stack.getItem() instanceof thaumcraft.api.IScribeTools || stack.getItem() instanceof TaintBottleItem; }
    public static int ink(ItemStack stack) { return stack.getItem() instanceof thaumcraft.api.IScribeTools tool ? tool.getInk(stack) : 0; }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos(); var state = level.getBlockState(pos);
        var player = context.getPlayer();
        if (player == null || !state.is(TCTables.TABLE.get())) return InteractionResult.PASS;
        // Original ItemInkwell excludes metadata 6 but permits the other extension halves.
        if (state.getValue(TableBlock.PART) == TableBlock.Part.MASTER
                || (state.getValue(TableBlock.PART) == TableBlock.Part.EXTENSION
                    && state.getValue(TableBlock.FACING) == net.minecraft.core.Direction.NORTH)) return InteractionResult.PASS;
        for (var direction : TableBlock.SEARCH_ORDER) {
            var other = pos.relative(direction);
            if (!level.hasChunkAt(other)) continue;
            var second = level.getBlockState(other);
            if (!second.is(TCTables.TABLE.get()) || second.getValue(TableBlock.PART) != TableBlock.Part.SINGLE) continue;
            if (!player.mayUseItemAt(pos, context.getClickedFace(), stack) || !player.mayUseItemAt(other, context.getClickedFace(), stack)) return InteractionResult.FAIL;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            level.setBlock(pos, state.setValue(TableBlock.PART, TableBlock.Part.MASTER).setValue(TableBlock.FACING, direction), Block.UPDATE_CLIENTS);
            level.setBlock(other, second.setValue(TableBlock.PART, TableBlock.Part.EXTENSION).setValue(TableBlock.FACING, direction.getOpposite()), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
                table.items.setStackInSlot(0, stack.copy());
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            // TC4 updates the client without notifying neighbors during conversion.
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
