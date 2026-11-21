package heartwork.content;

import heartwork.Heartwork;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public class HeartworkCrop extends CropBlock {
    private final String seed, produce;
    public HeartworkCrop(String seed, String produce) {
        super(BlockBehaviour.Properties.copy(Blocks.WHEAT));
        this.seed = seed; this.produce = produce;
    }
    @Override protected ItemLike getBaseSeedId() { return Heartwork.item(seed); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isMaxAge(state)) return InteractionResult.PASS;
        if (!player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand))) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            ItemStack harvest = new ItemStack(Heartwork.item(produce));
            if (!player.getInventory().add(harvest)) popResource(level, pos, harvest);
            player.giveExperiencePoints(5);
            level.setBlock(pos, getStateForAge(0), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
