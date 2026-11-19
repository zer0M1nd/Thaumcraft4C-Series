package thaumicfutures.mixin;

import com.buuz135.industrial.item.MobImprisonmentToolItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.content.PechEntity;

@Mixin(value = MobImprisonmentToolItem.class, remap = false)
public abstract class IndustrialPechMixin {
    @Inject(method = "getEntityFromStack", at = @At("RETURN"), remap = false)
    private void omniverse$emptyCopiedPack(ItemStack stack, Level level, boolean exact, boolean duplicator,
                                          CallbackInfoReturnable<Entity> ci) {
        // Releasing the captured original must keep its pack; only copies lose carried loot.
        if (duplicator && exact && ci.getReturnValue() instanceof PechEntity pech) {
            CompoundTag tag = new CompoundTag();
            pech.addAdditionalSaveData(tag);
            tag.put("Loot", new ListTag());
            pech.readAdditionalSaveData(tag);
        }
    }
}
