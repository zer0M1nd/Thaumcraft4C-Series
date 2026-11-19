package thaumicfutures.compat.exdeorum;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import tc4revival.content.EldritchRing;

/** An explicit player placement, with the whole generated footprint checked before mutation. */
final class EldritchPortalSpawner extends Item {
    EldritchPortalSpawner() { super(new Item.Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP || !ExDeorumCompat.enabled()) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos ground = context.getClickedPos();
        var snapshots = new ArrayList<BlockSnapshot>();
        for (BlockPos cursor : BlockPos.betweenClosed(ground.offset(-3,-4,-3),ground.offset(3,7,3))) {
            if (!level.hasChunkAt(cursor) || level.isOutsideBuildHeight(cursor) || !level.getWorldBorder().isWithinBounds(cursor)
                    || !level.mayInteract(player,cursor) || !player.mayUseItemAt(cursor,Direction.UP,context.getItemInHand())) return denied(context);
            var state=level.getBlockState(cursor);
            if (level.getBlockEntity(cursor)!=null || state.getDestroySpeed(level,cursor)<0) return denied(context);
            boolean terrain=state.is(BlockTags.DIRT)||state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.SAND)
                    ||state.is(Blocks.GRAVEL)||state.is(Blocks.PACKED_ICE)||state.is(Blocks.SNOW_BLOCK);
            if (cursor.getY()<=ground.getY() ? !(terrain||state.canBeReplaced()) : !(state.canBeReplaced()||state.is(BlockTags.LEAVES))) return denied(context);
            snapshots.add(BlockSnapshot.create(level.dimension(),level,cursor.immutable()));
        }
        if (ForgeEventFactory.onMultiBlockPlace(player,snapshots,Direction.UP)) return denied(context);
        int width=11+level.random.nextInt(6)*2, height=11+level.random.nextInt(6)*2;
        if (!EldritchRing.generate(level,ground,width,height,level.random)) return denied(context);
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
    private static InteractionResult denied(UseOnContext context) {
        context.getPlayer().displayClientMessage(Component.translatable("message.thaumicfutures.eldritch_portal_space"),true);
        return InteractionResult.FAIL;
    }
}
