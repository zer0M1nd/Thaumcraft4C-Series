package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class WardedBlockEntity extends BlockEntity {
    private BlockState original = Blocks.STONE.defaultBlockState();
    private UUID owner;
    public WardedBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.WARDED_ENTITY.get(), pos, state); }
    public BlockState original() { return original; }
    public UUID owner() { return owner; }
    public boolean ownedBy(Player player) { return player.getUUID().equals(owner); }
    public static boolean protect(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel(); if (!level.hasChunkAt(pos)) return false;
        var original = level.getBlockState(pos);
        // Legacy World.isBlockNormalCubeDefault checks opaque material and normal rendering,
        // not redstone conduction (which excludes redstone blocks in modern Minecraft).
        if (original.hasBlockEntity() || level.getBlockEntity(pos) != null || !original.isSolidRender(level, pos)) return false;
        if (net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos) < 0) return false;
        int light = original.getLightEmission(level, pos);
        if (!level.setBlockAndUpdate(pos, TCArcane.WARDED.get().defaultBlockState().setValue(WardedBlock.LIGHT, light))) return false;
        if (!(level.getBlockEntity(pos) instanceof WardedBlockEntity ward)) { level.setBlockAndUpdate(pos, original); return false; }
        ward.original = original; ward.owner = player.getUUID(); ward.setChanged();
        level.sendBlockUpdated(pos, ward.getBlockState(), ward.getBlockState(), 2); return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Original", NbtUtils.writeBlockState(original)); if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); original = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Original"));
        if (original.isAir() || original.hasBlockEntity() || original.is(TCArcane.WARDED.get())) original = Blocks.STONE.defaultBlockState();
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
