package thaumicomniverse.compat.backpack;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import tc4revival.aspect.*;
import tc4revival.content.EnergizedNodeBlockEntity;
import tc4revival.content.NodeConverterBlockEntity;

public final class EnergizedNodeUpgradeItem extends UpgradeItemBase<EnergizedNodeUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    public EnergizedNodeUpgradeItem() { super(Config.SERVER.maxUpgradesPerStorage); }
    @Override public UpgradeType<Wrapper> getType() { return TYPE; }
    @Override public List<UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getUpgradesPerStorage(String storageType) { return 1; }
    public static AspectAmounts rates(ItemStack stack) {
        return AspectNbt.read(stack.getOrCreateTag(), "OmniverseNodeCv", TCAspects.catalog()).amounts();
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos(); var stack = context.getItemInHand();
        if (stack.getCount() != 1 || stack.hasTag() && stack.getTag().contains("OmniverseNodeCv")
                || !(level.getBlockEntity(pos) instanceof EnergizedNodeBlockEntity node)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player))) return InteractionResult.FAIL;
        // AEB serializes the permanent supply when leases exist: temporary amplifiers cannot become permanent upgrades.
        var values = AspectNbt.read(node.saveWithoutMetadata(), "AEB", TCAspects.catalog()).amounts();
        var permanent = new AspectAmounts();
        for (var aspect : TCAspects.catalog().primals()) if (values.amount(aspect) > 0) permanent.add(aspect, values.amount(aspect));
        var converter = level.hasChunkAt(pos.above()) && level.getBlockEntity(pos.above()) instanceof NodeConverterBlockEntity value ? value : null;
        var previous = converter == null ? null : converter.saveWithoutMetadata();
        if (converter != null) {
            var reset = previous.copy(); reset.putInt("status", 0); reset.putInt("count", -1);
            converter.load(reset); converter.setChanged();
        }
        if (!level.removeBlock(pos, false)) {
            if (converter != null) { converter.load(previous); converter.setChanged(); }
            return InteractionResult.FAIL;
        }
        AspectNbt.write(stack.getOrCreateTag(), "OmniverseNodeCv", permanent);
        if (converter != null) level.sendBlockUpdated(pos.above(), converter.getBlockState(), converter.getBlockState(), 2);
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        if (!stack.hasTag() || !stack.getTag().contains("OmniverseNodeCv")) {
            lines.add(Component.translatable("omniverse.node_upgrade.empty")); return;
        }
        if (tc4revival.client.AspectTooltips.enabled()) return;
        var values = rates(stack);
        for (var aspect : TCAspects.catalog().primals()) if (values.amount(aspect) > 0)
            lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName() + ": " + values.amount(aspect) + " cv/t"));
    }
    public static final class Wrapper extends UpgradeWrapperBase<Wrapper, EnergizedNodeUpgradeItem> implements ITickableUpgrade {
        @Override public boolean isEnabled() { return thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled() && super.isEnabled(); }
        public Wrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) { super(storage, upgrade, save); }
        @Override public boolean hideSettingsTab() { return true; }
        public AspectAmounts rates() { return EnergizedNodeUpgradeItem.rates(upgrade); }
        @Override public void tick(Entity holder, Level level, BlockPos pos) {
            if(!isEnabled())return;
            if (level.isClientSide || !isEnabled()) return;
            var budget = rates();
            if (budget.isEmpty()) return;
            java.util.function.ToIntBiFunction<Aspect,Integer> supply=(aspect, requested) -> {
                int available = Math.min(budget.amount(aspect), requested); budget.reduce(aspect, available); return available;
            };
            BackpackVisCharging.charge(storageWrapper, supply, Integer.MAX_VALUE);
            if(holder instanceof net.minecraft.world.entity.player.Player player)
                BackpackVisCharging.chargePlayer(player,supply);
        }
    }
}
