package tbmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Legacy repair arithmetic, using modern menus, name packets and inventory synchronization. */
public final class MagicalAnvil extends AnvilBlock {
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 2);
    private final boolean voidMetal;
    public MagicalAnvil(boolean voidMetal) {
        super(Properties.of().strength(voidMetal ? 12 : 6, voidMetal ? Float.MAX_VALUE : 16).sound(SoundType.ANVIL).randomTicks());
        this.voidMetal = voidMetal; registerDefaultState(defaultBlockState().setValue(DAMAGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(DAMAGE); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (voidMetal && state.getValue(DAMAGE) > 0) level.setBlock(pos, state.setValue(DAMAGE, state.getValue(DAMAGE) - 1), 3);
    }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new Menu(id, inventory, ContainerLevelAccess.create(level, pos), voidMetal), Component.translatable(getDescriptionId()));
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) player.openMenu(getMenuProvider(state, level, pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void falling(net.minecraft.world.entity.item.FallingBlockEntity entity) {
        super.falling(entity); entity.getPersistentData().putInt("tbAnvilStartY", entity.blockPosition().getY());
    }
    @Override public void onLand(Level level, BlockPos pos, BlockState state, BlockState previous, net.minecraft.world.entity.item.FallingBlockEntity entity) {
        int distance = entity.getPersistentData().getInt("tbAnvilStartY") - pos.getY() - 1;
        if (!level.isClientSide && distance > 0 && level.random.nextFloat() < .05F + distance * .05F) {
            if (state.getValue(DAMAGE) == 2) { level.removeBlock(pos, false); level.levelEvent(1029, pos, 0); return; }
            level.setBlock(pos, state.setValue(DAMAGE, state.getValue(DAMAGE) + 1), 3);
        }
        super.onLand(level, pos, state, previous, entity);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (voidMetal) thaumcraft.api.fx.LegacyEffects.emit(level,"dark_sparkle",pos.getCenter(),net.minecraft.world.phys.Vec3.ZERO,0x101010,2,18,1,.5f,-.1f);
    }
    public static int itemDamage(ItemStack stack) {
        var tag = stack.getTagElement("BlockStateTag");
        if (tag == null) return 0;
        try { return Math.max(0, Math.min(2, Integer.parseInt(tag.getString("damage")))); }
        catch (NumberFormatException ignored) { return 0; }
    }
    public static final class Item extends BlockItem {
        public Item(Block block) { super(block, new net.minecraft.world.item.Item.Properties()); }
        @Override public String getDescriptionId(ItemStack stack) { return getDescriptionId() + "." + itemDamage(stack); }
    }
    public static final class Menu extends AnvilMenu {
        private final boolean voidMetal;
        private String newName;
        public Menu(int id, Inventory inventory, boolean voidMetal) { this(id, inventory, ContainerLevelAccess.NULL, voidMetal); }
        public Menu(int id, Inventory inventory, ContainerLevelAccess access, boolean voidMetal) { super(id, inventory, access); this.voidMetal = voidMetal; }
        @Override public MenuType<?> getType() { return voidMetal ? Content.VOID_ANVIL_MENU.get() : Content.THAUMIC_ANVIL_MENU.get(); }
        @Override protected boolean isValidBlock(BlockState state) { return state.getBlock() instanceof MagicalAnvil block && block.voidMetal == voidMetal; }
        @Override public boolean setItemName(String name) {
            // Keep vanilla packet sanitation/length limits while supplying the legacy pricing routine.
            newName = net.minecraft.SharedConstants.filterText(name).substring(0, Math.min(50, net.minecraft.SharedConstants.filterText(name).length()));
            boolean changed = super.setItemName(newName); createResult(); return changed;
        }
        private static int weight(Enchantment enchantment, boolean book) {
            int value = switch (enchantment.getRarity()) { case COMMON -> 1; case UNCOMMON -> 2; case RARE -> 4; case VERY_RARE -> 8; };
            return book ? Math.max(1, value / 2) : value;
        }
        @Override public void createResult() {
            ItemStack first = inputSlots.getItem(0), second = inputSlots.getItem(1);
            setMaximumCost(0); repairItemCountCost = 0; resultSlots.setItem(0, ItemStack.EMPTY);
            if (first.isEmpty()) return;
            ItemStack output = first.copy();
            Map<Enchantment,Integer> enchants = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(output));
            int extra = 0, rename = 0;
            long prior = (long)first.getBaseRepairCost() + (second.isEmpty() ? 0 : second.getBaseRepairCost());
            boolean book = false;
            if (!second.isEmpty()) {
                if (!net.minecraftforge.common.ForgeHooks.onAnvilChange(this, first, second, resultSlots, newName, (int)Math.min(Integer.MAX_VALUE, prior), player)) return;
                book = second.is(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantments(second).isEmpty();
                if (output.isDamageableItem() && output.getItem().isValidRepairItem(first, second)) {
                    int repaired = Math.min(output.getDamageValue(), output.getMaxDamage() / 4);
                    if (repaired <= 0) return;
                    while (repaired > 0 && repairItemCountCost < second.getCount()) {
                        output.setDamageValue(output.getDamageValue() - repaired);
                        extra += Math.max(1, repaired / 100) + enchants.size(); repairItemCountCost++;
                        repaired = Math.min(output.getDamageValue(), output.getMaxDamage() / 4);
                    }
                } else {
                    if (!book && (first.getItem() != second.getItem() || !output.isDamageableItem())) return;
                    if (output.isDamageableItem() && !book) {
                        int restored = second.getMaxDamage() - second.getDamageValue() + output.getMaxDamage() * 12 / 100;
                        int damage = Math.max(0, first.getDamageValue() - restored);
                        if (damage < output.getDamageValue()) { output.setDamageValue(damage); extra += Math.max(1, restored / 100); }
                    }
                    for (var entry : EnchantmentHelper.getEnchantments(second).entrySet()) {
                        Enchantment enchantment = entry.getKey();
                        int current = enchants.getOrDefault(enchantment, 0), incoming = entry.getValue();
                        int proposed = current == incoming ? incoming + 1 : Math.max(current, incoming), difference = proposed - current;
                        boolean compatible = enchantment.canEnchant(first) || player.getAbilities().instabuild || first.is(Items.ENCHANTED_BOOK);
                        for (Enchantment existing : enchants.keySet()) if (existing != enchantment && !enchantment.isCompatibleWith(existing)) { compatible = false; extra += difference; }
                        if (compatible) { enchants.put(enchantment, Math.min(proposed, enchantment.getMaxLevel())); extra += weight(enchantment, book) * difference; }
                    }
                }
            }
            if (newName == null || newName.isBlank()) {
                if (first.hasCustomHoverName()) { rename = first.isDamageableItem() ? 7 : first.getCount() * 5; extra += rename; output.resetHoverName(); }
            } else if (!newName.equals(first.getHoverName().getString())) {
                rename = first.isDamageableItem() ? 7 : first.getCount() * 5; extra += rename;
                if (first.hasCustomHoverName()) prior += rename / 2;
                output.setHoverName(Component.literal(newName));
            }
            int ordinal = 0;
            for (var entry : enchants.entrySet()) prior += ++ordinal + entry.getValue() * weight(entry.getKey(), book);
            if (book) prior = Math.max(1, prior / 2);
            if (book && !output.isBookEnchantable(second)) return;
            long price = voidMetal ? 3 : Math.min(Integer.MAX_VALUE, Math.max(0, prior + extra));
            if (!voidMetal && rename == extra && rename > 0 && price >= 60) price = 59;
            setMaximumCost((int)price);
            if (extra <= 0 || !voidMetal && price >= 60 && !player.getAbilities().instabuild) return;
            // The legacy computes a new repair penalty but never writes it: preserve the input penalty.
            EnchantmentHelper.setEnchantments(enchants, output);
            resultSlots.setItem(0, output); broadcastChanges();
        }
        @Override protected boolean mayPickup(Player player, boolean present) {
            return present && getCost() > 0 && (player.getAbilities().instabuild || player.experienceLevel >= getCost());
        }
        @Override protected void onTake(Player player, ItemStack result) {
            int price = getCost();
            if (!player.getAbilities().instabuild) player.giveExperienceLevels(-price);
            float chance = net.minecraftforge.common.ForgeHooks.onAnvilRepair(player, result, inputSlots.getItem(0), inputSlots.getItem(1)) / 3;
            int count = repairItemCountCost;
            ItemStack remainder = inputSlots.getItem(1).copy();
            if (count > 0 && remainder.getCount() > count) remainder.shrink(count); else remainder = ItemStack.EMPTY;
            inputSlots.setItem(0, ItemStack.EMPTY); inputSlots.setItem(1, remainder); setMaximumCost(0);
            access.execute((level, pos) -> {
                BlockState state = level.getBlockState(pos);
                // The old void menu checked the thaumic block ID: normal use never wears a void anvil.
                if (!voidMetal && !player.getAbilities().instabuild && isValidBlock(state) && player.getRandom().nextFloat() < chance) {
                    int damage = state.getValue(DAMAGE);
                    if (damage == 2) { level.removeBlock(pos, false); level.levelEvent(1029, pos, 0); return; }
                    level.setBlock(pos, state.setValue(DAMAGE, damage + 1), 3);
                }
                level.levelEvent(1030, pos, 0);
            });
        }
    }
}
