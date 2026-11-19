package thaumicfutures.compat.machinery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import thaumcraft.api.visnet.VisEndpoint;
import thaumcraft.api.visnet.VisNetHandler;

/** Storage units are whole essentia for essentia ports, centivis for vis ports and the bridge. */
public class AspectPort extends BlockEntity implements VisEndpoint {
    public enum Kind {
        ESSENTIA_INPUT, ESSENTIA_OUTPUT, VIS_INPUT, VIS_OUTPUT, BRIDGE;
        public boolean essentia() { return this == ESSENTIA_INPUT || this == ESSENTIA_OUTPUT; }
        public boolean input() { return this == ESSENTIA_INPUT || this == VIS_INPUT; }
    }
    private AspectAmounts stored = new AspectAmounts();
    private int enabledSides = 63;
    private Direction absorption;
    private boolean needsSync;
    public AspectPort(BlockPos pos, BlockState state) { super(MachineryContent.PORT_TILE.get(), pos, state); }
    public Kind kind() { return ((MachineryContent.PortBlock) getBlockState().getBlock()).kind; }
    public int tier() { return ((MachineryContent.PortBlock) getBlockState().getBlock()).tier; }
    public int capacity() { return kind() == Kind.BRIDGE ? 10000 : (kind().essentia() ? 64 : 100) << Math.min(10, tier() - 1); }
    public boolean accepts(Aspect aspect) { return aspect != null && (kind().essentia() || aspect.isPrimal()); }
    public int amount(Aspect aspect) { return aspect == null ? 0 : stored.amount(aspect); }
    public int space(Aspect aspect) { return accepts(aspect) ? Math.max(0, capacity() - amount(aspect)) : 0; }
    public AspectAmounts contents() { return stored.copy(); }
    public int insert(Aspect aspect, int request, boolean simulate) {
        if(!thaumicfutures.FutureFeatures.Group.MACHINERY.enabled())return 0;
        int accepted = Math.min(Math.max(0, request), space(aspect));
        if (accepted > 0 && !simulate) { stored.add(aspect, accepted); changed(); }
        return accepted;
    }
    public int extract(Aspect aspect, int request, boolean simulate) {
        int taken = Math.min(Math.max(0, request), amount(aspect));
        if (taken > 0 && !simulate) {
            stored.reduce(aspect, taken);
            if (stored.amount(aspect) == 0) stored.remove(aspect);
            changed();
        }
        return taken;
    }
    protected void replace(AspectAmounts values) {
        stored = new AspectAmounts();
        for (Aspect aspect : values.aspects()) if (accepts(aspect)) {
            int value = Math.max(0, Math.min(capacity(), values.amount(aspect)));
            if (value > 0) stored.add(aspect, value);
        }
        changed();
    }
    public boolean enabled(Direction face) { return face != null && (enabledSides & 1 << face.ordinal()) != 0; }
    public void toggleSide(Direction face) { enabledSides ^= 1 << face.ordinal(); refreshFaces(); changed(); }
    public Direction absorption() { return absorption; }
    public void selectAbsorption(Direction face) { absorption = absorption == face ? null : face; refreshFaces(); changed(); }
    private void refreshFaces() {
        if (level == null || level.isClientSide) return;
        BlockState next = getBlockState();
        for (Direction direction : Direction.values()) next = next.setValue(MachineryContent.PortBlock.FACES[direction.ordinal()],
                kind() == Kind.BRIDGE ? absorption == direction : enabled(direction));
        if (next != getBlockState()) level.setBlock(worldPosition, next, 3);
    }
    private void changed() {
        setChanged();
        needsSync = true;
    }
    public void tick() {
        if(!thaumicfutures.FutureFeatures.Group.MACHINERY.enabled())return;
        if (level == null || level.isClientSide) return;
        if (needsSync && level.getGameTime() % 5 == 0) {
            needsSync = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
        if (level.getGameTime() % 10 != 0) return;
        if (kind() == Kind.VIS_INPUT) {
            for (Aspect aspect : TCAspects.catalog().all()) if (aspect.isPrimal()) {
                int request = Math.min(10, space(aspect));
                if (request > 0) insert(aspect, VisNetHandler.drainVis(level, worldPosition,
                        thaumcraft.api.aspects.Aspect.getAspect(aspect.tag()), request), false);
            }
        } else if (kind() == Kind.BRIDGE && absorption != null) {
            BlockPos neighbor = worldPosition.relative(absorption);
            if (!level.hasChunkAt(neighbor) || !(level.getBlockEntity(neighbor) instanceof AspectPort port)
                    || port.kind() != Kind.VIS_OUTPUT || !port.enabled(absorption.getOpposite())) return;
            for (Aspect aspect : port.contents().aspects()) {
                int moving = Math.min(1000, space(aspect));
                if (moving > 0) insert(aspect, port.extract(aspect, moving, false), false);
            }
        } else if (this instanceof Essentia essentia && kind() == Kind.ESSENTIA_INPUT) essentia.pull();
    }
    @Override public void onLoad() { super.onLoad(); if (source() && valid()) VisNetHandler.register(this); }
    @Override public void setRemoved() { if (source() && level != null && !level.isClientSide) VisNetHandler.unregister(this); super.setRemoved(); }
    @Override public void onChunkUnloaded() { if (source() && level != null && !level.isClientSide) VisNetHandler.unregister(this); super.onChunkUnloaded(); }
    @Override public Level level() { return level; }
    @Override public Key key() { return new Key(worldPosition, ResourceLocation.parse("thaumicfutures:machine_vis_bridge")); }
    @Override public boolean valid() { return level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition); }
    @Override public boolean source() { return kind() == Kind.BRIDGE; }
    @Override public int range() { return 8; }
    @Override public int drain(Aspect aspect, int requested) { return thaumicfutures.FutureFeatures.Group.MACHINERY.enabled() && source() && accepts(aspect) ? extract(aspect, requested, false) : 0; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var values = new CompoundTag();
        for (Aspect aspect : stored.aspects()) values.putInt(aspect.tag(), stored.amount(aspect));
        tag.put("Aspects", values);
        tag.putInt("EnabledSides", enabledSides);
        tag.putInt("Absorption", absorption == null ? -1 : absorption.ordinal());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        stored = new AspectAmounts();
        var values = tag.getCompound("Aspects");
        for (String key : values.getAllKeys()) TCAspects.catalog().find(key).ifPresent(aspect -> {
            int amount = Math.max(0, Math.min(capacity(), values.getInt(key)));
            if (accepts(aspect) && amount > 0) stored.add(aspect, amount);
        });
        enabledSides = tag.contains("EnabledSides") ? tag.getInt("EnabledSides") & 63 : 63;
        int direction = tag.contains("Absorption") ? tag.getInt("Absorption") : -1;
        absorption = direction >= 0 && direction < 6 ? Direction.values()[direction] : null;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public static final class Essentia extends AspectPort implements thaumcraft.api.aspects.IEssentiaTransport,
            thaumcraft.api.aspects.IAspectContainer {
        public Essentia(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public thaumcraft.api.aspects.AspectList getAspects() { return thaumcraft.api.aspects.AspectList.from(contents()); }
        @Override public void setAspects(thaumcraft.api.aspects.AspectList values) { replace(values.unwrap()); }
        @Override public int capacity(Aspect aspect) { return capacity(); }
        @Override public boolean connectable(Direction face) { return enabled(face); }
        @Override public boolean canInput(Direction face) { return enabled(face) && kind().input(); }
        @Override public boolean canOutput(Direction face) { return enabled(face) && !kind().input(); }
        @Override public Aspect suctionType(Direction face) { return null; }
        @Override public int suction(Direction face) { return canInput(face) ? 128 : 0; }
        @Override public int minimumSuction() { return 0; }
        @Override public Aspect essentiaType(Direction face) { return enabled(face) ? contents().aspects().stream().findFirst().orElse(null) : null; }
        @Override public int essentiaAmount(Direction face) { return amount(essentiaType(face)); }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { return canOutput(face) ? extract(aspect, amount, false) : 0; }
        @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return canInput(face) ? insert(aspect, amount, false) : 0; }
        private void pull() {
            for (Direction direction : Direction.values()) {
                if (!canInput(direction)) continue;
                var pos = worldPosition.relative(direction);
                if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof tc4revival.content.EssentiaTransport source)) continue;
                Direction opposite = direction.getOpposite();
                Aspect aspect = source.essentiaType(opposite);
                if (!source.connectable(opposite) || !source.canOutput(opposite) || aspect == null || space(aspect) == 0
                        || source.essentiaAmount(opposite) <= 0 || source.suction(opposite) >= 128 || source.minimumSuction() > 128) continue;
                insert(aspect, source.takeEssentia(aspect, 1, opposite), false);
            }
        }
    }
}
