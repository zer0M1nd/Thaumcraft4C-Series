package thaumicomniverse.mixin;

import com.simibubi.create.content.trains.schedule.*;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.airship.AirshipContent;

@Mixin(value=ScheduleEditPacket.class,remap=false)
public abstract class CreateAirshipScheduleMixin {
    @Shadow private Schedule schedule;
    @Inject(method="handle",at=@At("HEAD"))
    private void omniverse$saveAirshipSchedule(NetworkEvent.Context context,CallbackInfoReturnable<Boolean> result){
        context.enqueueWork(()->{
            var player=context.getSender();if(player==null)return;var stack=player.getMainHandItem();
            // ScheduleScreen.removed sends this after its native close-container packet.
            // Match Create's held-item validation; the menu has already been closed.
            if(!stack.is(AirshipContent.SCHEDULE.get())||schedule.entries.size()>256)return;
            if(schedule.entries.isEmpty()){stack.removeTagKey("Schedule");}
            else stack.getOrCreateTag().put("Schedule",schedule.write());
            player.getCooldowns().addCooldown(stack.getItem(),5);
        });
    }
}
