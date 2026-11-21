package temodern.storage;

import java.util.List;
import appeng.api.config.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.*;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import temodern.essentia.*;
import temodern.TE;

/** A long-count storage implementation, shared by ordinary, quantum and creative cells. */
public final class EssentiaCell extends Item implements ICellWorkbenchItem {
    private final CellTier tier;
    public EssentiaCell(CellTier tier) { super(new Properties().stacksTo(1)); this.tier = tier; }
    public CellTier tier() { return tier; }
    private static java.util.Set<String> displayTags(ItemStack stack){
        var tags=new java.util.HashSet<String>();if(stack.hasTag())for(String key:java.util.List.of("essentia","essentia_recovery"))tags.addAll(stack.getTag().getCompound(key).getAllKeys());return tags;
    }
    private static boolean concealed(ItemStack stack){return displayTags(stack).stream().anyMatch(tag->!thaumcraft.api.client.ClientAspectViews.visible(tag));}
    @Override public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
        if(concealed(stack))return java.util.Optional.empty();
        var config=appeng.core.AEConfig.instance();
        if(!config.isTooltipShowCellContent())return java.util.Optional.empty();
        var contents=new java.util.ArrayList<GenericStack>();
        for(var entry:new Inventory(stack,this,null).getAvailableStacks())contents.add(new GenericStack(entry.getKey(),entry.getLongValue()));
        contents.sort(java.util.Comparator.comparingLong(GenericStack::amount).reversed().thenComparing(e->e.what().getId().toString()));
        int limit=Math.max(0,config.getTooltipMaxCellContentShown());boolean more=contents.size()>limit;
        if(more)contents.subList(limit,contents.size()).clear();
        return java.util.Optional.of(new appeng.items.storage.StorageCellTooltipComponent(List.of(),List.copyOf(contents),more,true));
    }
    @Override public ConfigInventory getConfigInventory(ItemStack stack) {
        return CellConfig.create(EssentiaKeyType.INSTANCE.filter(), stack, tier.types());
    }
    @Override public FuzzyMode getFuzzyMode(ItemStack stack) { return FuzzyMode.IGNORE_ALL; }
    @Override public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!player.isShiftKeyDown()||tier.creative()||tier.id().equals("quantum")||tier.id().equals("singularity")||!new Inventory(stack,this,null).canFitInsideCell())return net.minecraft.world.InteractionResultHolder.pass(stack);
        if(!level.isClientSide){
            var component=new ItemStack(TE.MATERIALS.get("storage_component_"+tier.id()).get());
            player.setItemInHand(hand,component);player.getInventory().placeItemBackInInventory(new ItemStack(TE.MATERIALS.get("storage_casing").get()));
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        if(concealed(stack)){lines.add(thaumcraft.api.client.ClientAspectViews.unknownContents(displayTags(stack)));return;}
        var storage = new Inventory(stack, this, null);
        lines.add(Component.translatable("gui.thaumicenergistics.cell_capacity", storage.total(), tier.capacity()));
        lines.add(Component.translatable("gui.thaumicenergistics.cell_types", storage.values().size(), tier.types()));
        if (tier.creative()) lines.add(Component.translatable("gui.thaumicenergistics.creative_cell"));
    }
    public static final class Handler implements ICellHandler {
        @Override public boolean isCell(ItemStack stack) { return stack.getItem() instanceof EssentiaCell; }
        @Override public StorageCell getCellInventory(ItemStack stack, ISaveProvider host) {
            return stack.getItem() instanceof EssentiaCell cell ? new Inventory(stack, cell, host) : null;
        }
    }
    public static final class Inventory implements StorageCell {
        private final ItemStack stack;
        private final EssentiaCell item;
        private final ISaveProvider host;
        public Inventory(ItemStack stack, EssentiaCell item, ISaveProvider host) { this.stack = stack; this.item = item; this.host = host; }
        private CompoundTag raw(String name) { return stack.hasTag() ? stack.getTag().getCompound(name) : new CompoundTag(); }
        /** Read-only normalization: simulation, tooltips and capacity queries never modify the stack. */
        private CompoundTag values() {
            var result=new CompoundTag();
            for(var source:List.of(raw("essentia_recovery"),raw("essentia")))for(var key:source.getAllKeys())
                if(source.contains(key,net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)&&source.getLong(key)>0&&EssentiaKey.parse(key)!=null)result.putLong(key,source.getLong(key));
            return result;
        }
        private CompoundTag unresolved() {
            var result=new CompoundTag();
            for(var source:List.of(raw("essentia_recovery"),raw("essentia")))for(var key:source.getAllKeys())
                if(!source.contains(key,net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)||source.getLong(key)<=0||EssentiaKey.parse(key)==null)result.put(key,source.get(key).copy());
            return result;
        }
        private boolean hasUnresolvedContents() {
            var saved=unresolved();
            return saved.getAllKeys().stream().anyMatch(key->saved.getLong(key)>0);
        }
        public long total() {
            long sum = 0;
            var values = values();
            for (String key : values.getAllKeys()) {
                long count = Math.max(0, values.getLong(key));
                // Treat corrupt or externally edited overfull contents as full, never wrap negative.
                if (count > Long.MAX_VALUE - sum) return Long.MAX_VALUE;
                sum += count;
            }
            return sum;
        }
        private boolean allows(EssentiaKey key) {
            var filter = item.getConfigInventory(stack).keySet();
            return filter.isEmpty() || filter.contains(key);
        }
        private void put(String tag, long amount) {
            var values = values();
            if (amount == 0) values.remove(tag); else values.putLong(tag, amount);
            commit(values);
        }
        private void commit(CompoundTag values) {
            var recovery=unresolved();
            if(values.equals(raw("essentia"))&&recovery.equals(raw("essentia_recovery")))return;
            var tag=stack.getOrCreateTag();tag.put("essentia",values);
            if(recovery.isEmpty())tag.remove("essentia_recovery");else tag.put("essentia_recovery",recovery);
            if(host!=null)host.saveChanges();
        }
        @Override public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            if (!temodern.RevivalFeatures.tierEnabled(item.tier.id())) return 0;
            if (!(what instanceof EssentiaKey key) || key.aspect() == null || !allows(key)) return 0;
            if (item.tier.creative()) return amount;
            var values = values();
            if (!values.contains(key.tag()) && values.size() >= item.tier.types()) return 0;
            long accepted = Math.min(amount, Math.max(0, item.tier.capacity() - total()));
            if (accepted > 0 && mode == Actionable.MODULATE) put(key.tag(), Math.addExact(values.getLong(key.tag()), accepted));
            return accepted;
        }
        @Override public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            if (!(what instanceof EssentiaKey key) || key.aspect() == null) return 0;
            if (item.tier.creative()) return allows(key) ? amount : 0;
            long stored = Math.max(0, values().getLong(key.tag()));
            long taken = Math.min(amount, stored);
            if (taken > 0 && mode == Actionable.MODULATE) put(key.tag(), stored - taken);
            return taken;
        }
        @Override public void getAvailableStacks(KeyCounter out) {
            if (item.tier.creative()) {
                TCAspects.catalog().all().stream().map(EssentiaKey::of).filter(this::allows).forEach(key -> out.add(key, item.tier.capacity()));
            } else {
                var values = values();
                for (String tag : values.getAllKeys()) {
                    var key = EssentiaKey.parse(tag);
                    if (key != null && values.getLong(tag) > 0) out.add(key, values.getLong(tag));
                }
            }
        }
        @Override public Component getDescription() { return stack.getHoverName(); }
        @Override public CellState getStatus() {
            if (item.tier.creative() || total() >= item.tier.capacity()) return CellState.FULL;
            if (values().isEmpty()) return CellState.EMPTY;
            return values().size() >= item.tier.types() ? CellState.TYPES_FULL : CellState.NOT_EMPTY;
        }
        @Override public double getIdleDrain() { return item.tier.idle(); }
        @Override public boolean canFitInsideCell() { return !item.tier.creative() && values().isEmpty() && !hasUnresolvedContents(); }
        @Override public void persist() { commit(values()); }
    }
}
