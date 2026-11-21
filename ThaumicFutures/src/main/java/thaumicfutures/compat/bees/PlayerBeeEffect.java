package thaumicfutures.compat.bees;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IGenome;
import forestry.apiculture.genetics.effects.ThrottledBeeEffect;
import forestry.core.genetics.EffectData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PlayerBeeEffect implements IBeeEffect {
    public enum Action { SPEED, HASTE, CLEANSE }
    private final Action action;
    public PlayerBeeEffect(Action action) { this.action = action; }
    @Override public boolean isDominant() { return false; }
    @Override public IEffectData validateStorage(IEffectData storage) {
        return storage instanceof EffectData data && data.getIntSize() >= 1 ? data : new EffectData(1, 0);
    }
    @Override public IEffectData doEffect(IGenome genome, IEffectData storage, IBeeHousing housing) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return storage;
        storage = validateStorage(storage);
        if (housing.getWorldObj().isClientSide) return storage;
        int elapsed = storage.getInteger(0);
        if (elapsed < 200) { storage.setInteger(0, elapsed + 1); return storage; }
        storage.setInteger(0, 0);
        for (Player player : ThrottledBeeEffect.getEntitiesInRange(genome, housing, Player.class)) {
            if (player.isSpectator()) continue;
            if (action == Action.CLEANSE) player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            else player.addEffect(new MobEffectInstance(action == Action.SPEED ? MobEffects.MOVEMENT_SPEED : MobEffects.DIG_SPEED,
                    action == Action.SPEED ? 200 : 300));
        }
        return storage;
    }
}
