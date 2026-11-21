package thaumicomniverse.compat.gregtech;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Stable recipe/network representation independent of the world's current aspect objects. */
public record EssentiaFuel(String aspect,int amount) {
    public static final Codec<EssentiaFuel> CODEC=RecordCodecBuilder.create(instance->instance.group(
            Codec.STRING.fieldOf("aspect").forGetter(EssentiaFuel::aspect),
            Codec.intRange(1,1_048_576).fieldOf("amount").forGetter(EssentiaFuel::amount)).apply(instance,EssentiaFuel::new));
    public EssentiaFuel { if(aspect==null||aspect.isBlank()||amount<1)throw new IllegalArgumentException("Invalid essentia fuel"); }
}
