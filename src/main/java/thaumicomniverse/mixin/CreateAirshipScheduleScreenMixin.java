package thaumicomniverse.mixin;

import com.simibubi.create.content.trains.schedule.*;
import java.util.*;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.airship.*;

@Mixin(value=ScheduleScreen.class,remap=false)
public abstract class CreateAirshipScheduleScreenMixin {
    @ModifyArg(method={"renderInput","renderBg","m_7286_"},at=@At(value="INVOKE",target="Lnet/createmod/catnip/gui/element/GuiGameElement;of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/catnip/gui/element/GuiGameElement$GuiRenderBuilder;"),index=0,require=2)
    private ItemStack omniverse$stationIcon(ItemStack stack){return AirshipSchedulePresentation.icon(stack);}
    @Inject(method="getViableStations",at=@At("HEAD"),cancellable=true)
    private void omniverse$airshipStations(IScheduleInput field,CallbackInfoReturnable<List<IntAttached<String>>> ci){
        if(AirshipSchedulePresentation.active())ci.setReturnValue(AirshipClient.scheduleStations());
    }
}
