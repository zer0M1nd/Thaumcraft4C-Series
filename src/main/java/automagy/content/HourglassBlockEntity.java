package automagy.content;

import automagy.logic.HourglassTimer;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HourglassBlockEntity extends BlockEntity implements MenuProvider {
    public final HourglassTimer timer = new HourglassTimer();
    private boolean firstTick = true, notifying;
    private long lastManualFlip = Long.MIN_VALUE;
    public long clientFlipTime = -100;
    public long levelTime() { return level == null ? 0 : level.getGameTime(); }
    public HourglassBlockEntity(BlockPos pos, BlockState state) { super(Content.HOURGLASS_TILE.get(), pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, HourglassBlockEntity be) {
        if (level.isClientSide) {
            boolean inverted = be.timer.flipped(); be.timer.tick();
            if (inverted != be.timer.flipped()) be.clientFlipTime = level.getGameTime();
            return;
        }
        if (be.firstTick) { be.timer.initialInput(be.powered()); be.firstTick = false; }
        else be.refreshInput();
        int signal = be.timer.signal(), comparator = be.timer.comparator();
        boolean inverted = be.timer.flipped(), running = be.timer.running();
        be.timer.tick();
        if (running || be.timer.pulse() > 0) be.setChanged();
        if (signal != be.timer.signal() || comparator != be.timer.comparator() || inverted != be.timer.flipped()) be.changed();
        else if (running && level.getGameTime() % 20 == 0) be.sync();
    }
    private boolean powered() {
        if (level == null) return false;
        for (Direction dir : Direction.values())
            if ((dir.getAxis() == Direction.Axis.Y || dir.getAxis() == getBlockState().getValue(HourglassBlock.FACING).getAxis())
                    && level.hasSignal(worldPosition.relative(dir), dir)) return true;
        return false;
    }
    public void refreshInput() {
        if (notifying || level == null || level.isClientSide) return;
        boolean power = powered();
        if (firstTick) { timer.initialInput(power); firstTick = false; setChanged(); }
        else if (power != timer.input()) { timer.setInput(power); changed(); }
    }
    public void flip() {
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        if (lastManualFlip != Long.MIN_VALUE && now - lastManualFlip < 5) return;
        if (timer.flip()) { lastManualFlip = now; changed(); }
    }
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            sync();
            if (!notifying) {
                notifying = true;
                try { level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock()); }
                finally { notifying = false; }
            }
        }
    }
    private void sync() { level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Target", timer.target()); tag.putInt("Elapsed", timer.elapsed()); tag.putInt("Pulse", timer.pulse());
        tag.putInt("Signal", timer.signalMode()); tag.putInt("Flip", timer.flipMode()); tag.putInt("InputMode", timer.inputMode());
        tag.putBoolean("Input", timer.input()); tag.putBoolean("Flipped", timer.flipped()); tag.putBoolean("Repeat", timer.repeat());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (!tag.contains("Target")) return;
        boolean previousFlipped = timer.flipped();
        timer.restore(tag.getInt("Target"), tag.getInt("Elapsed"), tag.getInt("Pulse"), tag.getInt("Signal"), tag.getInt("Flip"),
                tag.getInt("InputMode"), tag.getBoolean("Input"), tag.getBoolean("Flipped"), tag.getBoolean("Repeat"));
        firstTick = false;
        if (level != null && level.isClientSide && previousFlipped != timer.flipped()) clientFlipTime = level.getGameTime();
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("block.automagy.magic_hourglass"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new HourglassMenu(id, inventory, this); }
}
