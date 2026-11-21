package thaumicomniverse.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.content.AspectOrbEntity;
import thaumicomniverse.compat.backpack.BackpackOrbCollection;

@Mixin(value = MagnetUpgradeWrapper.class, remap = false)
public abstract class BackpackMagnetMixin {
    @Shadow private boolean canNotPickup(Entity pickup, Entity holder) { throw new AssertionError(); }
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void omniverse$visOrbs(Entity holder, Level level, BlockPos pos, CallbackInfo ci) {
        var self = (MagnetUpgradeWrapper) (Object) this;
        if (level.isClientSide || level.getGameTime() % 10 != 0 || !self.isEnabled() || !self.shouldPickupXp()) return;
        int radius = ((MagnetUpgradeItem) self.getUpgradeStack().getItem()).getRadius();
        for (var orb : level.getEntitiesOfClass(AspectOrbEntity.class, new AABB(pos).inflate(radius))) {
            if (canNotPickup(orb, holder)) continue;
            if (holder instanceof Player player) orb.playerTouch(player);
            if (orb.isAlive()) BackpackOrbCollection.collect(((BackpackWrapperAccess) this).omniverse$storage(), orb);
        }
    }
}
