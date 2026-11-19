package temodern.mixin;

import java.util.Arrays;
import appeng.api.config.TypeFilter;
import appeng.api.storage.AEKeyFilter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import temodern.essentia.EssentiaKeyType;

/** Extend AE's native setting so every terminal shares its cycling, sync and persistence. */
@Mixin(value = TypeFilter.class, remap = false)
public abstract class TypeFilterMixin {
    @Shadow @Final @Mutable private static TypeFilter[] $VALUES;

    @Invoker("<init>")
    private static TypeFilter thaumicenergistics$create(String name, int ordinal, AEKeyFilter filter) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void thaumicenergistics$addEssentia(CallbackInfo ci) {
        var values = Arrays.copyOf($VALUES, $VALUES.length + 1);
        values[values.length - 1] = thaumicenergistics$create(
                "THAUMICENERGISTICS_ESSENTIA", $VALUES.length, EssentiaKeyType.INSTANCE.filter());
        $VALUES = values;
    }
}
