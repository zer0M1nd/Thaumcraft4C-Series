package automagy.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class TallyLensItem extends Item {
    private final String kind;
    public TallyLensItem(String kind) { super(new Properties()); this.kind = kind; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos(); var player = context.getPlayer();
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()) || kind.equals("blank")) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof TallyBlockEntity tile) || !((TallyBlock)tile.getBlockState().getBlock()).kind.equals("inventory")) return InteractionResult.PASS;
        if (!level.isClientSide) {
            var state = tile.getBlockState(); boolean all = tile.requireAll;
            ItemStack filter = tile.inventory.extractItem(0, 1, false);
            var upgraded = Content.TALLIES.get(kind).get().defaultBlockState().setValue(TallyBlock.FACING, state.getValue(TallyBlock.FACING));
            if (level.setBlock(pos, upgraded, 3) && level.getBlockEntity(pos) instanceof TallyBlockEntity target) {
                target.requireAll = all; target.inventory.setStackInSlot(0, filter); target.sync(); target.refresh();
                if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            } else if (level.getBlockEntity(pos) instanceof TallyBlockEntity original) original.inventory.setStackInSlot(0, filter);
            else if (!filter.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), filter);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
