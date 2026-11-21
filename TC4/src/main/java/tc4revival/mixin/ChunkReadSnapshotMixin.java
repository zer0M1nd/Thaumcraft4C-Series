package tc4revival.mixin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pending IOWorker writes must not be exposed to chunk loading/data fixing. */
@Mixin(value = ChunkStorage.class, remap = false)
public abstract class ChunkReadSnapshotMixin {
    @Inject(method = {"read(Lnet/minecraft/world/level/ChunkPos;)Ljava/util/concurrent/CompletableFuture;",
            "m_223454_(Lnet/minecraft/world/level/ChunkPos;)Ljava/util/concurrent/CompletableFuture;"},
            at = @At("RETURN"), cancellable = true, remap = false)
    private void thaumcraft$readSnapshot(ChunkPos pos,
            CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> callback) {
        // loadAsync may return the exact tag still queued for writing. Data fixing
        // adds/removes __context even for current-version chunks, racing that write.
        callback.setReturnValue(callback.getReturnValue().thenApply(tag -> tag.map(CompoundTag::copy)));
    }
}
