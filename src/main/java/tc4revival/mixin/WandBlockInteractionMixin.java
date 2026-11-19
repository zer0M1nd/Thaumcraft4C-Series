package tc4revival.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
import tc4revival.content.CastingWandItem;
import thaumcraft.api.wands.WandAutomation;

/** Covers ordinary block callbacks too, including automation that calls BlockState.use directly. */
@Mixin(value=BlockBehaviour.BlockStateBase.class, remap=false)
public abstract class WandBlockInteractionMixin {
    @Group(name="wandBlockInteraction", min=1, max=1)
    @Redirect(method={"use", "m_60664_"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/Block;use(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"), require=0, remap=false)
    private InteractionResult thaumcraft$named(Block block, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(block,state,level,pos,player,hand,hit);
    }
    @Group(name="wandBlockInteraction", min=1, max=1)
    @Redirect(method={"use", "m_60664_"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/Block;m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"), require=0, remap=false)
    private InteractionResult thaumcraft$srg(Block block, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(block,state,level,pos,player,hand,hit);
    }
    private static InteractionResult interact(Block block, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var stack=player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CastingWandItem)) return block.use(state,level,pos,player,hand,hit);
        if (!WandAutomation.allowed(stack,player)) return InteractionResult.FAIL;
        try (var scope=WandAutomation.open(stack,player)) {
            return block.use(state,level,pos,player,hand,hit);
        }
    }
}
