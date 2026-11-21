package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public  class ThaumiumTools {
    private ThaumiumTools() {}
    private static Item.Properties properties() { return new Item.Properties().rarity(Rarity.UNCOMMON); }
    public static Item create(ThaumiumToolType type) {
        return switch (type) {
            case PICKAXE -> new Pick(); case AXE -> new Axe(); case SHOVEL -> new Shovel(); case HOE -> new Hoe();
            case SWORD -> new ThaumiumSwordItem();
        };
    }
    static class Pick extends PickaxeItem implements RepairableItem {
        Pick() { this(ThaumiumToolTier.INSTANCE); }
        Pick(Tier tier) { super(tier, 2, -2.8f, properties()); }
        @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
    }
    static class Axe extends AxeItem implements RepairableItem {
        Axe() { this(ThaumiumToolTier.INSTANCE); }
        Axe(Tier tier) { super(tier, 3, -3, properties()); }
        @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
    }
    static class Shovel extends ShovelItem implements RepairableItem {
        Shovel() { this(ThaumiumToolTier.INSTANCE); }
        Shovel(Tier tier) { super(tier, 1, -3, properties()); }
        @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
    }
    static class Hoe extends HoeItem implements RepairableItem {
        Hoe() { this(ThaumiumToolTier.INSTANCE); }
        Hoe(Tier tier) { super(tier, -(int) tier.getAttackDamageBonus(), 0, properties()); }
        @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
        @Override public int getEnchantmentValue() { return 5; }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return 1; }
        @Override public boolean isCorrectToolForDrops(BlockState state) { return false; }
        @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return false; }
        @Override public boolean canPerformAction(ItemStack stack, ToolAction action) { return action == ToolActions.HOE_TILL; }
        @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return false; }
        @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) { return false; }
        @Override public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos();
            var face = context.getClickedFace(); var stack = context.getItemInHand();
            if (player == null || !player.mayUseItemAt(pos, face, stack)
                    || face == Direction.DOWN || !level.isEmptyBlock(pos.above())) return InteractionResult.PASS;
            var state = level.getBlockState(pos);
            // In 1.7.10 both coarse dirt and podzol were metadata forms of the same dirt block.
            if (!state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.DIRT) && !state.is(Blocks.COARSE_DIRT) && !state.is(Blocks.PODZOL))
                return InteractionResult.PASS;
            var farmland = Blocks.FARMLAND.defaultBlockState();
            var sound = farmland.getSoundType(level, pos, player);
            level.playSound(player, pos, sound.getStepSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8f);
            if (!level.isClientSide) {
                level.setBlock(pos, farmland, 3);
                stack.hurtAndBreak(1, player, wearer -> wearer.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
