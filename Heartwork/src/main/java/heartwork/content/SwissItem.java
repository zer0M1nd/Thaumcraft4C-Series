package heartwork.content;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.*;

public class SwissItem extends Item {
    private final String mode;
    public SwissItem(int durability, String mode) { super(new Properties().durability(durability)); this.mode = mode; }
    public String mode() { return mode; }
    public static boolean active(ItemStack stack) { return stack.hasTag() && stack.getTag().getBoolean("active"); }
    public static int proficiency(ItemStack stack) { return Math.max(1, Math.min(4, stack.hasTag() ? stack.getTag().getInt("Exp") / 25 : 0)); }

    protected void wear(ItemStack stack, LivingEntity user, int cost, int experience) {
        stack.hurtAndBreak(cost / proficiency(stack), user, actor -> actor.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        if (!stack.isEmpty()) {
            var tag = stack.getOrCreateTag();
            tag.putInt("Exp", (int)Math.min(Integer.MAX_VALUE, Math.max(0L, tag.getInt("Exp")) + experience));
        }
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (mode.equals("crafter") && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                wear(stack, player, 12, 0);
                if (!stack.isEmpty()) player.openMenu(new SimpleMenuProvider((id, inventory, who) ->
                        new CraftingMenu(id, inventory, ContainerLevelAccess.create(level, player.blockPosition())) {
                            @Override public boolean stillValid(Player user) { return user.getItemInHand(hand) == stack && !stack.isEmpty(); }
                        }, Component.translatable("container.crafting")));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.isShiftKeyDown() || mode.equals("crafter")) {
            if (!level.isClientSide) stack.getOrCreateTag().putBoolean("active", !active(stack));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!active(stack)) return 0;
        if (mode.equals("knife")) return 1;
        int rank = proficiency(stack);
        boolean combat = mode.equals("combat");
        if (state.is(Blocks.COBWEB) || state.is(BlockTags.LEAVES)) return combat ? 40 * rank : 20;
        if (state.is(BlockTags.WOOL)) return (combat ? 25 : 10) * rank;
        if (state.is(Blocks.OBSIDIAN)) return (combat ? 1.25F : 0.5F) * rank;
        if (state.is(BlockTags.LOGS)) return (combat ? 20 : 10) * rank;
        return (combat ? 3 : 2) * rank;
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return active(stack); }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
        if (!level.isClientSide && active(stack) && state.getDestroySpeed(level, pos) != 0) wear(stack, user, 4, mode.equals("knife") ? 0 : 1);
        return true;
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) wear(stack, attacker, 4, mode.equals("knife") ? 0 : 1);
        return true;
    }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();
        return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Heartwork Swiss damage", active(stack) ? 5 : 0, AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Heartwork Swiss speed", -2.4, AttributeModifier.Operation.ADDITION));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !active(stack)) return InteractionResult.PASS;
        if (!player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (mode.equals("combat") || mode.equals("tools")) {
            // Delegate to Forge's actual tool interaction, including cancellation and modded tool transformations.
            ItemStack delegate = stack.copy();
            delegate.setDamageValue(0);
            UseOnContext delegated = new UseOnContext(context.getLevel(),player,context.getHand(),delegate,
                    new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()));
            InteractionResult result = (mode.equals("combat") ? Items.FLINT_AND_STEEL : Items.IRON_HOE).useOn(delegated);
            if (!context.getLevel().isClientSide && result.consumesAction() && !stack.isEmpty()) {
                wear(stack, player, 4, mode.equals("tools") ? 2 : 1);
            }
            return result;
        }
        return InteractionResult.PASS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!mode.equals("tools") || !active(stack) || !(entity instanceof IForgeShearable shearable)) return InteractionResult.PASS;
        BlockPos pos = entity.blockPosition();
        if (!shearable.isShearable(stack, entity.level(), pos)) return InteractionResult.PASS;
        if (!entity.level().isClientSide) {
            int fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, stack);
            for (ItemStack drop : shearable.onSheared(player, stack, entity.level(), pos, fortune)) entity.spawnAtLocation(drop, 1);
            wear(stack, player, 4, 1);
        }
        return InteractionResult.sidedSuccess(entity.level().isClientSide);
    }
    @Override public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return active(stack) && (mode.equals("tools") && (ToolActions.DEFAULT_HOE_ACTIONS.contains(action) || ToolActions.DEFAULT_SHEARS_ACTIONS.contains(action))
                || mode.equals("combat") && ToolActions.DEFAULT_SWORD_ACTIONS.contains(action));
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide && !mode.equals("knife") && holder instanceof LivingEntity && holder.tickCount % 400 == 0 && stack.isDamaged())
            stack.setDamageValue(stack.getDamageValue() - 1);
    }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return ingredient.is(heartwork.Heartwork.item("hone")); }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(active(stack) ? "heartwork.swiss.open" : "heartwork.swiss.closed"));
        if (!mode.equals("knife")) lines.add(Component.translatable("heartwork.swiss.proficiency", proficiency(stack), stack.hasTag() ? stack.getTag().getInt("Exp") : 0));
        lines.add(Component.translatable("heartwork.swiss.use." + mode));
    }
}
