package thaumicomniverse.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

/** Applied only on a dedicated server; the host's client sound implementation remains intact. */
@Pseudo
@Mixin(targets="com.yuo.endless.items.tool.InfinityBow",remap=false)
public abstract class EndlessServerBowSoundMixin {
    /**
     * @author TC4Revival
     * @reason Endless 0.8.1's common item verifier resolves SoundInstance in this client-only body.
     */
    @Overwrite(remap=false)
    public static void playChargeSound(Player player) {}

    /**
     * @author TC4Revival
     * @reason Dedicated servers have no local sound manager; keep client audio on the client.
     */
    @Overwrite(remap=false)
    public static void stopChargeSound(Player player) {}
}
