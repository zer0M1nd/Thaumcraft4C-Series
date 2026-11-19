package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Server-side bookshelf transformation; the wand dispatch checks for an installed focus first. */
public  class WandBookCreation {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BOOKSHELVES = net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:thaumonomicon_bookshelves"));
    private WandBookCreation() {}
    public static InteractionResult create(ServerPlayer player, BlockPos position) {
        var level = player.serverLevel();
        if (!level.hasChunkAt(position) || !level.getBlockState(position).is(BOOKSHELVES)
                || !level.mayInteract(player, position)) return InteractionResult.PASS;
        var item = TCEntities.SPECIAL_ITEM.get().create(level);
        if (item == null) return InteractionResult.FAIL;
        item.setPos(position.getX() + .5, position.getY() + .3, position.getZ() + .5);
        item.setItem(new ItemStack(TCItems.THAUMONOMICON.get())); item.setDeltaMovement(Vec3.ZERO);
        var before = level.getBlockState(position);
        if (!level.removeBlock(position, false)) return InteractionResult.FAIL;
        if (!level.addFreshEntity(item)) {
            level.setBlock(position, before, 3); return InteractionResult.FAIL;
        }
        level.playSound(null, position, TCSounds.WAND.get(), SoundSource.MASTER, 1, 1);
        return InteractionResult.SUCCESS;
    }
}
