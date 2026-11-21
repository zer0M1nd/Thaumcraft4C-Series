package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Creative monument helper with complete placement validation. */
public  class ObeliskPlacerItem extends Item {
    public ObeliskPlacerItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        if (context.getClickedFace()!=Direction.UP || context.getPlayer()==null || !context.getPlayer().getAbilities().instabuild) return InteractionResult.PASS;
        var level=context.getLevel(); var pos=context.getClickedPos(); var player=context.getPlayer(); player.swing(context.getHand());
        for (int y=1;y<=7;y++) {
            var target=pos.above(y);
            if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target) || !level.mayInteract(player,target) || !player.mayUseItemAt(target,Direction.UP,stack)) return InteractionResult.FAIL;
            if (!level.isEmptyBlock(target)) return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos.above(),TCEldritch.ALTAR.get().defaultBlockState(),3);
            level.setBlock(pos.above(3),TCEldritch.OBELISK.get().defaultBlockState(),3);
            for (int y=4;y<=7;y++) level.setBlock(pos.above(y),TCEldritch.OBELISK_SEGMENT.get().defaultBlockState(),3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("item.thaumcraft.aura_node.creative").withStyle(ChatFormatting.ITALIC));
    }
}
