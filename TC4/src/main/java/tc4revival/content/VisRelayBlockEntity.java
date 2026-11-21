package tc4revival.content;

import java.util.HashSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.Aspect;

public class VisRelayBlockEntity extends VisNetworkNodeBlockEntity implements WandInteractable {
    private static final java.util.Map<Player, java.lang.ref.WeakReference<VisRelayBlockEntity>> NEARBY = new java.util.WeakHashMap<>();
    private static final String[] PRIMALS = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};
    private byte color = -1;
    private BlockPos clientParent;
    private Vec3 clientParentPoint;
    private int pulse;
    private float red = .5f, green = .5f, blue = .5f;
    private float beamOpacity = .3f;
    public VisRelayBlockEntity(BlockPos pos, BlockState state) { this(TCArcane.VIS_RELAY_ENTITY.get(), pos, state); }
    protected VisRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public int range() { return 8; }
    @Override public boolean source() { return false; }
    @Override public byte attunement() { return color; }
    public boolean charger() { return false; }
    public Direction orientation() { return getBlockState().getValue(VisRelayBlock.FACING); }
    public Vec3 beamPoint() { return Vec3.atCenterOf(worldPosition).subtract(Vec3.atLowerCornerOf(orientation().getNormal()).scale(.05)); }
    public VisNetworkNodeBlockEntity displayedParent() {
        if (level == null || !level.isClientSide) return parent();
        return clientParent != null && level.hasChunkAt(clientParent)
                && level.getBlockEntity(clientParent) instanceof VisNetworkNodeBlockEntity node ? node : null;
    }
    public Vec3 displayedParentPoint() {
        if(level==null)return null;
        if(!level.isClientSide){var parent=networkParent();return parent==null?null:parent.point();}
        var nativeParent=displayedParent();
        if(nativeParent instanceof VisRelayBlockEntity relay)return relay.beamPoint();
        return clientParent!=null && level.hasChunkAt(clientParent)?clientParentPoint:null;
    }
    public boolean pulsing() { return pulse > 0; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float beamOpacity() { return beamOpacity; }
    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            red = Math.min(1, red + .025f); green = Math.min(1, green + .025f); blue = Math.min(1, blue + .025f);
            beamOpacity = pulse > 0 ? .8f : Math.max(.3f, beamOpacity - .025f);
        }
        if (pulse > 0) pulse--;
        tickNetwork();
        if (!level.isClientSide && networkTicks % 20 == 0) {
            for (var player : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(5))) {
                var reference = NEARBY.get(player); var previous = reference == null ? null : reference.get();
                if (VisNetwork.live(previous) && previous.getLevel() == level && player.position().distanceToSqr(Vec3.atCenterOf(previous.getBlockPos()))
                        < player.position().distanceToSqr(Vec3.atCenterOf(worldPosition))) continue;
                NEARBY.put(player, new java.lang.ref.WeakReference<>(this));
            }
        }
    }
    public static VisRelayBlockEntity nearby(Player player) {
        if (player.level().isClientSide) return null;
        var reference = NEARBY.get(player); var relay = reference == null ? null : reference.get();
        if (VisNetwork.live(relay) && relay.getLevel() == player.level() && player.position().distanceToSqr(Vec3.atCenterOf(relay.getBlockPos())) < 26) return relay;
        NEARBY.remove(player); return null;
    }
    public void setAttunement(int selected) {
        if (level == null || level.isClientSide) return;
        color = (byte) (selected >= 0 && selected < 6 ? selected : -1);
        refreshNetwork(); setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        level.playSound(null, worldPosition, TCSounds.CRYSTAL.get(), SoundSource.BLOCKS, .2f, 1);
    }
    @Override protected void onParentChanged() {
        if (level == null || level.isClientSide || charger()) return;
        // Disconnect is also called while the containing block is being removed.
        // Do not restore the old relay state over the replacement block in that callback.
        if (!level.hasChunkAt(worldPosition) || !level.getBlockState(worldPosition).is(getBlockState().getBlock())) return;
        boolean connected = networkParent() != null;
        if (getBlockState().getValue(VisRelayBlock.CONNECTED) != connected)
            level.setBlock(worldPosition, getBlockState().setValue(VisRelayBlock.CONNECTED, connected), 3);
    }
    @Override protected void onVisConsumed(Aspect aspect) {
        if (level == null || pulse != 0) return;
        for (int i = 0; i < PRIMALS.length; i++) if (PRIMALS[i].equals(aspect.tag())) {
            pulse = 5; level.blockEvent(worldPosition, getBlockState().getBlock(), 0, i); break;
        }
    }
    private void pulse(int index) {
        int rgb = ShardType.values()[index].tint(); pulse = 5;
        red = (rgb >> 16 & 255) / 255f; green = (rgb >> 8 & 255) / 255f; blue = (rgb & 255) / 255f;
    }
    @Override public boolean triggerEvent(int id, int value) {
        if (id != 0 || value < 0 || value >= 6) return super.triggerEvent(id, value);
        if (level != null && level.isClientSide) {
            pulse(value); var seen = new HashSet<BlockPos>(); seen.add(worldPosition);
            var node = displayedParent();
            while (node instanceof VisRelayBlockEntity relay && seen.add(relay.getBlockPos()) && !relay.pulsing()) {
                relay.pulse(value); node = relay.displayedParent();
            }
        }
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putByte("color", color);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); color = tag.contains("color") ? tag.getByte("color") : -1;
        if (color < -1 || color > 5) color = -1;
        clientParent = tag.contains("Parent", net.minecraft.nbt.Tag.TAG_LONG) ? BlockPos.of(tag.getLong("Parent")) : null;
        if(clientParent!=null && clientParent.distSqr(worldPosition)>81)clientParent=null;
        clientParentPoint=clientParent==null?null:tag.contains("ParentX")?new Vec3(tag.getDouble("ParentX"),tag.getDouble("ParentY"),tag.getDouble("ParentZ")):Vec3.atCenterOf(clientParent);
        if(clientParentPoint!=null && (!Double.isFinite(clientParentPoint.x)||!Double.isFinite(clientParentPoint.y)||!Double.isFinite(clientParentPoint.z)||clientParentPoint.distanceToSqr(Vec3.atCenterOf(worldPosition))>81)) {clientParent=null;clientParentPoint=null;}
    }
    @Override public CompoundTag getUpdateTag() {
        var tag = saveWithoutMetadata(); var parent = networkParent();
        if (parent != null) {
            tag.putLong("Parent", parent.key().position().asLong());var point=parent.point();
            tag.putDouble("ParentX",point.x);tag.putDouble("ParentY",point.y);tag.putDouble("ParentZ",point.z);
        }
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public InteractionResult onWandUseFirst(ItemStack wand, UseOnContext context) {
        setAttunement(color + 1); return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public Optional<ItemStack> onWandUse(Level level, ItemStack wand, Player player) { return Optional.empty(); }
    @Override public void onWandUseTick(ItemStack wand, Player player, int remainingTicks) {}
    @Override public void onWandRelease(ItemStack wand, Level level, Player player, int remainingTicks) {}
}
