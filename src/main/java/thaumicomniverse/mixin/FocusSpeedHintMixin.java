package thaumicomniverse.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import thaumicomniverse.compat.create.FocusSpeedScreen;

@Mixin(value=ValueSettingsScreen.class,remap=false)
public abstract class FocusSpeedHintMixin {
    @ModifyConstant(method="renderWindow",constant=@Constant(stringValue="key.use"))
    private String omniverse$confirmKey(String key){
        return (Object)this instanceof FocusSpeedScreen?"key.thaumcraft.change_area":key;
    }
}
