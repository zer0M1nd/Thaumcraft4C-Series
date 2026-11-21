package thaumicomniverse.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla reads block entity NBT through the built-in registry, bypassing Forge's missing-ID aliases. */
@Mixin(value = BlockEntity.class, remap = false)
public abstract class LegacyConvenientTubeMixin {
    @Inject(method = {"loadStatic", "m_155241_"}, at = @At("HEAD"), require = 1)
    private static void omniverse$migrateTube(BlockPos position, BlockState state, CompoundTag tag,
                                              CallbackInfoReturnable<BlockEntity> result) {
        if ("thaumcraft:convenient_essentia_tube".equals(tag.getString("id")))
            tag.putString("id", "thaumicomniverse:convenient_essentia_tube");
    }
}
