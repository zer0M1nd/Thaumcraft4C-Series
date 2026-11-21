package thaumicomniverse.compat.create.airship;

import com.simibubi.create.content.trains.schedule.ScheduleItem;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The native schedule editor/menu and NBT, with an independent item and vehicle contract. */
public final class AirshipScheduleItem extends ScheduleItem {
    public AirshipScheduleItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,Player player,InteractionHand hand){
        if(!player.isShiftKeyDown()&&hand==InteractionHand.MAIN_HAND&&player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)AirshipNetwork.scheduleStations(serverPlayer);
        return super.use(level,player,hand);
    }
    @Override public InteractionResult handScheduleTo(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(target!=null&&target.getRootVehicle() instanceof AirshipEntity ship)
            return AirshipSchedule.interact(ship,player,hand)?InteractionResult.SUCCESS:InteractionResult.PASS;
        return InteractionResult.PASS;
    }
}
