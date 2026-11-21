package heartwork.content;

import heartwork.Heartwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class StrawItem extends Item {
    public StrawItem() { super(new Properties()); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        var entity = Heartwork.STRAW.get().create(context.getLevel());
        if (entity == null) return InteractionResult.FAIL;
        entity.moveTo(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5, player.getYRot(), 0);
        if (!context.getLevel().noCollision(entity)) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide) {
            context.getLevel().addFreshEntity(entity);
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
