package thaumicfutures.mixin;

import com.buuz135.industrial.block.agriculturehusbandry.tile.MobCrusherTile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
import tc4revival.content.GolemEntity;
import tc4revival.content.TravelingTrunkEntity;

@Mixin(value = MobCrusherTile.class, remap = false)
public abstract class IndustrialCrusherMixin {
    // IF's blacklist only replaces instant death with damage, whereas MFR skipped these mobs entirely.
    @Group(name = "omniverse$crusher", min = 1, max = 1)
    @Redirect(method = "work", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_45976_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"), remap = false, require = 0)
    private <T extends Entity> List<T> omniverse$skipConstructs(Level level, Class<T> type, AABB box) {
        var entities = new ArrayList<>(level.getEntitiesOfClass(type, box));
        entities.removeIf(entity -> entity instanceof GolemEntity || entity instanceof TravelingTrunkEntity);
        return entities;
    }
    @Group(name = "omniverse$crusher", min = 1, max = 1)
    @Redirect(method = "work", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"), remap = false, require = 0)
    private <T extends Entity> List<T> omniverse$skipConstructsDev(Level level, Class<T> type, AABB box) {
        return omniverse$skipConstructs(level, type, box);
    }
}
