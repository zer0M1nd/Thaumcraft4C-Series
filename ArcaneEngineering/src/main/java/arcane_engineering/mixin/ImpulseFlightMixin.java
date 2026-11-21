package arcane_engineering.mixin;

import arcane_engineering.BootsNetwork;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep flight validation compatible with mods that widen the connection fields through an AT. */
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
public abstract class ImpulseFlightMixin {
    @Unique private static final java.lang.reflect.Field arcaneEngineering$floating =
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class,"f_9736_");
    @Unique private static final java.lang.reflect.Field arcaneEngineering$aboveGround =
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class,"f_9737_");

    @Inject(method={"tick()V","m_9933_()V"},at=@At("HEAD"),remap=false)
    private void arcaneEngineering$authorizedThrust(CallbackInfo callback) {
        var connection=(ServerGamePacketListenerImpl)(Object)this;
        if(BootsNetwork.hasFlightPermit(connection.player)) {
            try {
                arcaneEngineering$floating.setBoolean(connection,false);
                arcaneEngineering$aboveGround.setInt(connection,0);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot reset authorized impulse flight",failure);
            }
        }
    }
}
