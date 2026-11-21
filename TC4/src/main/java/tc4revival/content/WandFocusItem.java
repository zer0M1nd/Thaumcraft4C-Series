package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Shared focus dispatch contract; concrete foci supply their actual effects. */
public abstract class WandFocusItem extends Item {
    public enum Animation { WAVE, CHARGE }
    public String depthTexture() { return null; }
    /** Stack-sensitive artwork; retain the no-argument entry for existing addons. */
    public String depthTexture(ItemStack focus) { return depthTexture(); }
    public String ornamentTexture() { return null; }
    public String ornamentTexture(ItemStack focus) { return ornamentTexture(); }
    protected WandFocusItem() { super(new Properties().stacksTo(1)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    public abstract int color(ItemStack focus);
    public abstract java.util.Map<tc4revival.aspect.Aspect, Integer> costs(ItemStack focus);
    public boolean costPerTick(ItemStack focus) { return false; }
    public int cooldownMillis(ItemStack focus) { return 0; }
    public Animation animation(ItemStack focus) { return Animation.WAVE; }
    public abstract java.util.List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank);
    public  java.util.List<FocusUpgrade> availableUpgrades(ItemStack focus,int rank) {
        var result=new java.util.LinkedHashSet<>(upgradesForRank(focus,rank));
        for(var upgrade:FocusUpgrade.values())if(upgrade.offered(focus,rank))result.add(upgrade);
        return java.util.List.copyOf(result);
    }
    public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade upgrade, int rank) { return true; }
    public int nextRank(ItemStack focus) {
        var values = appliedUpgrades(focus); int rank = 1;
        while (rank <= 5 && values[rank - 1] != -1) rank++;
        return rank;
    }
    public short[] appliedUpgrades(ItemStack focus) {
        short[] result = {-1, -1, -1, -1, -1};
        if (focus.hasTag()) {
            var entries = focus.getTag().getList("upgrade", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(5, entries.size()); i++) result[i] = (short)FocusUpgrade.savedId(entries.getCompound(i));
        }
        return result;
    }
    public boolean applyUpgrade(ItemStack focus, FocusUpgrade upgrade, int rank) {
        if (rank < 1 || rank > 5 || upgrade == null) return false;
        var values = appliedUpgrades(focus);
        if (values[rank - 1] != -1) return false;
        values[rank - 1] = (short) upgrade.id;
        var entries = new net.minecraft.nbt.ListTag();
        for (short id : values) { var entry = new CompoundTag(); entry.putShort("id", id); FocusUpgrade.byId(id).ifPresent(value->entry.putString("key",value.key)); entries.add(entry); }
        focus.getOrCreateTag().put("upgrade", entries); thaumcraft.api.wands.FocusUpgradeType.applied(upgrade,focus,rank); return true;
    }
    public void appendUpgrades(ItemStack focus, java.util.List<net.minecraft.network.chat.Component> lines) {
        var counts = new java.util.LinkedHashMap<Short, Integer>();
        for (short id : appliedUpgrades(focus)) if (id >= 0) counts.merge(id, 1, Integer::sum);
        counts.forEach((id, amount) -> FocusUpgrade.byId(id).ifPresent(upgrade -> {
            var text = net.minecraft.network.chat.Component.translatable(upgrade.nameKey()).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE);
            if (amount > 1) text.append(" ").append(net.minecraft.network.chat.Component.translatable("enchantment.level." + amount));
            lines.add(text);
        }));
    }
    @Override public void appendHoverText(ItemStack focus, Level level, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flags) {
        var costs = costs(focus); var format = new java.text.DecimalFormat("#####.##");
        if (!costs.isEmpty() && !tc4revival.client.AspectTooltips.enabled()) {
            lines.add(net.minecraft.network.chat.Component.translatable(costPerTick(focus) ? "item.Focus.cost2" : "item.Focus.cost1"));
            costs.entrySet().stream().sorted(java.util.Comparator.comparing(entry -> entry.getKey().tag())).forEach(entry -> {
                var aspect = entry.getKey();
                lines.add(net.minecraft.network.chat.Component.literal(" " + thaumcraft.api.aspects.AspectViews.display(aspect).displayName()).withStyle(style -> style.withColor(thaumcraft.api.aspects.AspectViews.display(aspect).color()))
                        .append(net.minecraft.network.chat.Component.literal(" x " + format.format(entry.getValue() / 100f)).withStyle(net.minecraft.ChatFormatting.RESET)));
            });
        }
        appendUpgrades(focus, lines);
    }
    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (stack.hasTag()) { stack.getTag().remove("ench"); stack.getTag().remove("Enchantments"); }
    }
    public abstract String sortingPrefix();
    public String sortingKey(ItemStack focus) {
        var key = new StringBuilder(sortingPrefix());
        var upgrades = focus.hasTag() ? focus.getTag().getList("upgrade", Tag.TAG_COMPOUND) : new net.minecraft.nbt.ListTag();
        for (int rank = 0; rank < 5; rank++) key.append(rank < upgrades.size() ? FocusUpgrade.savedId(upgrades.getCompound(rank)) : -1);
        return key.toString();
    }
    public abstract InteractionResultHolder<ItemStack> cast(ItemStack wand, Level level, Player player, InteractionHand hand, BlockHitResult hit);
    public void usingTick(ItemStack wand, Player player, int remaining) {}
    public void release(ItemStack wand, Level level, Player player, int remaining) {}
    public boolean swing(ItemStack wand, LivingEntity entity) { return false; }
    public boolean startBreak(ItemStack wand, BlockPos position, Player player) { return false; }
    public int upgradeLevel(ItemStack focus, int upgradeId) {
        if (!focus.hasTag()) return 0;
        var upgrades = focus.getTag().getList("upgrade", Tag.TAG_COMPOUND);
        int count = 0;
        for (int i = 0; i < Math.min(5, upgrades.size()); i++) if (FocusUpgrade.savedId(upgrades.getCompound(i)) == upgradeId) count++;
        return count;
    }
    public static ItemStack attached(ItemStack wand) {
        return thaumcraft.api.wands.WandCasting.attached(wand).orElseGet(()->physical(wand));
    }
    public static ItemStack physical(ItemStack wand) {
        if (!wand.hasTag() || !wand.getTag().contains("focus", Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        var focus = ItemStack.of(wand.getTag().getCompound("focus"));
        return focus.getItem() instanceof WandFocusItem ? focus : ItemStack.EMPTY;
    }
    public static void attach(ItemStack wand, ItemStack focus) {
        if(thaumcraft.api.wands.WandCasting.commit(wand,focus))return;
        attachPhysical(wand,focus);
    }
    public static void attachPhysical(ItemStack wand,ItemStack focus) {
        if (focus.isEmpty()) { if (wand.hasTag()) wand.getTag().remove("focus"); return; }
        if (!(focus.getItem() instanceof WandFocusItem)) throw new IllegalArgumentException("Not a wand focus");
        var single = focus.copy(); single.setCount(1);
        wand.getOrCreateTag().put("focus", single.save(new CompoundTag()));
    }
}
