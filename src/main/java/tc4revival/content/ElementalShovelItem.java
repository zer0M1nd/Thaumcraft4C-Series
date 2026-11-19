package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import tc4revival.network.FocusEffectPacket;

public  class ElementalShovelItem extends ShovelItem implements RepairableItem {
    private Direction side=Direction.DOWN;
    public ElementalShovelItem() { super(ElementalToolTier.INSTANCE,1,-3,new Properties().rarity(Rarity.RARE)); }
    // Block placement owns right-click even when the inventory has no usable blocks.
    @Override public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction action) {
        return action == net.minecraftforge.common.ToolActions.SHOVEL_DIG;
    }
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player) {
        var start=player.getEyePosition();
        var hit=player.level().clip(new ClipContext(start,start.add(player.getLookAngle().scale(10)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,player));
        if (hit.getType()==HitResult.Type.BLOCK) side=hit.getDirection();
        return false;
    }
    private static boolean effective(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(Blocks.FARMLAND) || state.is(Blocks.SOUL_SAND);
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity user) {
        if (user.isShiftKeyDown()) return super.mineBlock(stack,level,state,pos,user);
        if (user instanceof ServerPlayer player && effective(state)) {
            for (var target:ElementalShovelLayout.plane(pos,side,0,player.getYRot())) {
                if (!level.hasChunkAt(target) || !level.mayInteract(player,target)) continue;
                var candidate=level.getBlockState(target);
                if (candidate.getDestroySpeed(level,target)<0 || !effective(candidate)) continue;
                stack.hurtAndBreak(1,user,owner->owner.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                ElementalHarvest.harvest(player,target,3);
            }
        }
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel(); var player=context.getPlayer(); var clicked=context.getClickedPos();
        if (player==null || level.getBlockEntity(clicked)!=null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean changed=false;
        var copied=level.getBlockState(clicked); var stack=context.getItemInHand();
        for (var target:ElementalShovelLayout.placement(level,clicked,context.getClickedFace(),ElementalShovelLayout.orientation(stack),player.getYRot())) {
            if (!level.mayInteract(player,target) || !player.mayUseItemAt(target,context.getClickedFace(),stack)) continue;
            var placed=copied; int color=8401408;
            if (!player.isCreative() && !consume(player,copied.getBlock().asItem())) {
                if (!copied.is(Blocks.GRASS_BLOCK) || !consume(player,Items.DIRT)) continue;
                placed=Blocks.DIRT.defaultBlockState(); color=3;
            }
            var sound=copied.getSoundType(level,clicked,player);
            level.playSound(null,target,sound.getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,.6f,.9f+level.random.nextFloat()*.2f);
            changed|=level.setBlock(target,placed,3);
            stack.hurtAndBreak(1,player,owner->owner.broadcastBreakEvent(context.getHand()));
            FocusEffectPacket.blockSparkle(level,target,color,4); player.swing(context.getHand(),true);
        }
        // Forge commits captured block snapshots/notifications only for a successful action.
        return changed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
    private static boolean consume(Player player,Item item) {
        if (item==Items.AIR) return false;
        for (var stack:player.getInventory().items) if (stack.is(item) && !stack.isEmpty()) { stack.shrink(1); return true; }
        return false;
    }
}
