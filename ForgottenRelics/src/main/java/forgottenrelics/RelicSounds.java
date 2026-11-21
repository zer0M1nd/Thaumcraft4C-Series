package forgottenrelics;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class RelicSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ForgottenRelics.ID);
    public static final RegistryObject<SoundEvent> DISCHARGE = add("discharge"),
            LUNAR = add("lunar_flare"),
            STARFALL = add("starfall"),
            VOID = add("void_charge");

    private static RegistryObject<SoundEvent> add(String path) {
        return SOUNDS.register(
                path, () -> SoundEvent.createVariableRangeEvent(ForgottenRelics.id(path)));
    }

    private RelicSounds() {}
}
