package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Restorable block state and a one-slice-per-tick tunnel front; block entities are never displaced. */
public  class PortableHoleBlockEntity extends BlockEntity {
    private BlockState original = Blocks.AIR.defaultBlockState();
    private int age, maximum = 120, remaining, direction = -1;
    private UUID caster;
    public PortableHoleBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.PORTABLE_HOLE_ENTITY.get(), pos, state); }
    public static boolean open(ServerPlayer player, BlockPos pos, int direction, int count, int duration) {
        var level = player.serverLevel();
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        if (PortableHoleFocusItem.stops(level, pos, state) || state.hasBlockEntity() || level.getBlockEntity(pos) != null
                || state.canBeReplaced() || !state.blocksMotion() || !level.mayInteract(player, pos)
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return false;
        // Honour server protection without harvesting, drops, block snapshots or temporary inventory removal.
        if (net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos) < 0) return false;
        if (!level.setBlock(pos, TCArcane.PORTABLE_HOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) return false;
        if (!(level.getBlockEntity(pos) instanceof PortableHoleBlockEntity hole)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE); return false;
        }
        hole.original = state; hole.maximum = duration; hole.remaining = count; hole.direction = direction; hole.caster = player.getUUID();
        hole.setChanged(); level.sendBlockUpdated(pos, hole.getBlockState(), hole.getBlockState(), Block.UPDATE_CLIENTS); return true;
    }
    public void tick() {
        if (level == null) return;
        if (level.isClientSide) { tc4revival.client.HoleEffects.tick(level, worldPosition, age == 0 || age == maximum - 1); age++; return; }
        if (age == 0 && remaining > 1 && direction >= 0 && caster != null) {
            var player = level.getServer().getPlayerList().getPlayer(caster);
            if (player != null && player.level() == level) {
                var face = Direction.from3DDataValue(direction);
                for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    var side = switch (face.getAxis()) { case X -> worldPosition.offset(0, a, b); case Y -> worldPosition.offset(a, 0, b); case Z -> worldPosition.offset(a, b, 0); };
                    open(player, side, -1, 1, maximum);
                }
                open(player, worldPosition.relative(face.getOpposite()), direction, remaining - 1, maximum);
            }
        }
        age++; setChanged();
        if (age >= maximum && level.getBlockEntity(worldPosition) == this) {
            level.setBlock(worldPosition, original, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (!original.isAir()) level.scheduleTick(worldPosition, original.getBlock(), 2);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Original", NbtUtils.writeBlockState(original));
        tag.putInt("countdown", age); tag.putInt("countdownmax", maximum); tag.putInt("count", remaining); tag.putInt("direction", direction);
        if (caster != null) tag.putUUID("Caster", caster);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); original = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Original"));
        // Malformed memory must not restore an uninitialised hole or an inventory block with lost contents.
        if (original.is(TCArcane.PORTABLE_HOLE.get()) || original.hasBlockEntity()) original = Blocks.AIR.defaultBlockState();
        maximum = Mth.clamp(tag.getInt("countdownmax"), 1, 420); age = Mth.clamp(tag.getInt("countdown"), 0, maximum);
        remaining = Mth.clamp(tag.getInt("count"), 0, 74); direction = Mth.clamp(tag.getInt("direction"), -1, 5);
        caster = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
