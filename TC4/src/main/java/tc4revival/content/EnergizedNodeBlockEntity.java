package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

public  class EnergizedNodeBlockEntity extends VisNetworkNodeBlockEntity implements RevealedAspectContainer, ScannableNode {
    private AspectAmounts aura = new AspectAmounts(), supply = new AspectAmounts(), available = new AspectAmounts();
    private record AuraLease(AspectAmounts bonus, long expires) {}
    private final java.util.Map<java.util.UUID, AuraLease> amplifiers = new java.util.HashMap<>();
    private AspectAmounts spentThisTick = new AspectAmounts();
    private AuraNodeType type = AuraNodeType.NORMAL;
    private AuraNodeModifier modifier;
    private String nodeId = "blank";
    private long suppliedAt = Long.MIN_VALUE;
    public EnergizedNodeBlockEntity(BlockPos pos, BlockState state) {
        super(TCArcane.ENERGIZED_NODE_ENTITY.get(), pos, state);
        for (String name : new String[]{"aer", "ignis", "terra", "aqua", "ordo", "perditio"}) aura.add(TCAspects.catalog().require(name), 20);
    }
    @Override public int range() { return 8; }
    @Override public boolean source() { return true; }
    public AspectAmounts aura() { return aura.copy(); }
    public AspectAmounts supply() { expireAmplifiers(); return supply.copy(); }
    /** Permanent aura is always aura(); temporary additive bonuses are available here only. */
    public AspectAmounts effectiveAura() { expireAmplifiers(); return combinedAura(); }
    public int activeAmplifierCount() { expireAmplifiers(); return amplifiers.size(); }
    /** Renewable server-only lease identified by a device instance UUID. Not serialized.
     * TTL is 1..1200 ticks. Values are nonnegative primal aura, capped at one million each.
     */
    public boolean amplify(java.util.UUID source, AspectAmounts bonus, int ticks) {
        java.util.Objects.requireNonNull(source); java.util.Objects.requireNonNull(bonus);
        if (ticks < 1 || ticks > 1200 || bonus.isEmpty()) throw new IllegalArgumentException("Invalid amplifier lease");
        for (var aspect : bonus.aspects()) if (!aspect.isPrimal() || bonus.amount(aspect) < 0 || bonus.amount(aspect) > 1000000)
            throw new IllegalArgumentException("Invalid amplifier aura");
        if (!thaumcraft.api.LiveBlockEntity.server(this)) return false;
        expireAmplifiers();
        if (!amplifiers.containsKey(source) && amplifiers.size() >= 1024) return false;
        var previous = amplifiers.put(source, new AuraLease(bonus.copy(), level.getGameTime() + ticks));
        if (previous == null || !previous.bonus.view().equals(bonus.view())) rebuildSupply();
        return true;
    }
    public boolean removeAmplifier(java.util.UUID source) {
        if (!thaumcraft.api.LiveBlockEntity.server(this)) return false;
        if (amplifiers.remove(source) == null) return false;
        rebuildSupply(); return true;
    }
    public void clearAmplifiers() {
        if (thaumcraft.api.LiveBlockEntity.server(this) && !amplifiers.isEmpty()) { amplifiers.clear(); rebuildSupply(); }
    }
    private void expireAmplifiers() {
        if (level != null && !level.isClientSide && amplifiers.values().removeIf(lease -> lease.expires <= level.getGameTime())) rebuildSupply();
    }
    private AspectAmounts combinedAura() {
        var result = aura.copy();
        for (var lease : amplifiers.values()) for (var aspect : lease.bonus.aspects()) {
            long total = (long)result.amount(aspect) + lease.bonus.amount(aspect);
            result.remove(aspect); result.add(aspect, (int)Math.min(Integer.MAX_VALUE, total));
        }
        return result;
    }
    @Override public AspectAmounts aspects() { return supply(); }
    @Override public AspectAmounts revealedAspects() { return supply(); }
    public AuraNodeType nodeType() { return type; }
    public AuraNodeModifier nodeModifier() { return modifier; }
    public String nodeId() { return nodeId; }
    public void configure(AspectAmounts aura, AuraNodeType type, AuraNodeModifier modifier, String id) {
        this.aura = aura.copy(); this.type = type; this.modifier = modifier; nodeId = id;
        spentThisTick = new AspectAmounts(); suppliedAt = Long.MIN_VALUE;
        rebuildSupply();
    }
    public void tick() {
        tickNetwork();
        if (level == null || level.isClientSide) return;
        if (type == AuraNodeType.UNSTABLE && level.random.nextInt(500) == 1) supply = new AspectAmounts();
        replenish();
    }
    private void replenish() {
        if (level == null) return;
        expireAmplifiers();
        if (supply.isEmpty() && !aura.isEmpty()) rebuildSupply();
        if (suppliedAt != level.getGameTime()) { available = supply.copy(); spentThisTick = new AspectAmounts(); suppliedAt = level.getGameTime(); }
    }
    private AspectAmounts calculateSupply(AspectAmounts input, boolean randomize) {
        var reduced = TCAspects.catalog().toPrimals(input, AspectCatalog.Reduction.MAXIMUM);
        var result = new AspectAmounts();
        for (var aspect : reduced.aspects()) {
            double amount = Math.max(0, reduced.amount(aspect));
            if (modifier != null) amount *= switch (modifier) { case BRIGHT -> 1.2f; case PALE -> .8f; case FADING -> .5f; };
            // Preserve integer rounding before the old float sqrt/floor operation.
            int output = (int)Math.floor((float)Math.sqrt((int)Math.min(Integer.MAX_VALUE, amount)));
            if (randomize && type == AuraNodeType.UNSTABLE && level != null) output += level.random.nextInt(5) - 2;
            if (output > 0) result.add(aspect, output);
        }
        return result;
    }
    private void rebuildSupply() {
        supply = calculateSupply(combinedAura(), true);
        if (level != null && suppliedAt == level.getGameTime()) {
            available = new AspectAmounts();
            for (var aspect : supply.aspects()) available.add(aspect, Math.max(0, supply.amount(aspect) - spentThisTick.amount(aspect)));
        }
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected int drainSource(Aspect aspect, int requested) {
        replenish(); int drained = Math.min(available.amount(aspect), requested);
        if (drained > 0) { available.reduce(aspect, drained); spentThisTick.add(aspect, drained); }
        return drained;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putString("nodeId", nodeId); tag.putByte("type", (byte) type.ordinal());
        tag.putByte("modifier", modifier == null ? -1 : (byte) modifier.ordinal());
        AspectNbt.write(tag, aura); AspectNbt.write(tag, "AEB", amplifiers.isEmpty() ? supply : calculateSupply(aura, false));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); nodeId = tag.getString("nodeId"); type = AuraNodeType.fromId(tag.getByte("type"));
        modifier = AuraNodeModifier.fromId(tag.getByte("modifier")); aura = AspectNbt.read(tag, TCAspects.catalog()).amounts();
        supply = AspectNbt.read(tag, "AEB", TCAspects.catalog()).amounts();
        for (var aspect : aura.aspects()) if (aura.amount(aspect) < 0) aura.remove(aspect);
        for (var aspect : supply.aspects()) if (!aspect.isPrimal() || supply.amount(aspect) <= 0) supply.remove(aspect);
        amplifiers.clear(); spentThisTick = new AspectAmounts(); available = new AspectAmounts(); suppliedAt = Long.MIN_VALUE;
    }
    @Override public CompoundTag getUpdateTag() {
        var tag = saveWithoutMetadata(); AspectNbt.write(tag, "AEB", supply); return tag;
    }
    @Override public void setRemoved() { amplifiers.clear(); supply = new AspectAmounts(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { amplifiers.clear(); supply = new AspectAmounts(); super.onChunkUnloaded(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
