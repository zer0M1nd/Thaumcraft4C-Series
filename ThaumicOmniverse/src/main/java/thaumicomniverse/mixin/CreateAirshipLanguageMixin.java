package thaumicomniverse.mixin;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import thaumicomniverse.compat.create.airship.AirshipSchedulePresentation;

/** Translate only newly-created components inside the airship menu, leaving language caches intact. */
@Mixin(value=TranslatableContents.class,remap=false)
public abstract class CreateAirshipLanguageMixin {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private static String omniverse$airshipWords(String key){return AirshipSchedulePresentation.key(key);}
}
