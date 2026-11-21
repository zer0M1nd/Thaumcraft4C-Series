package thaumicomniverse.compat;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import tc4revival.content.VisCharging;
import thaumcraft.api.devices.VisChargingTargets;
import thaumicomniverse.compat.create.VisItemRepair;

/** Tablet identity is optional; no Tinkerer or Create class is required to register this adapter. */
public final class TabletVisCharging {
    private TabletVisCharging() {}
    public static void setup(){
        VisChargingTargets.register("thaumicomniverse:tablet",0,context->{
            if(!context.target().getClass().getName().equals("ttmodern.TabletEntity"))return Optional.empty();
            return Optional.of(payment->{
                var inventory=payment.target().getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElse(null);
                if(inventory==null||inventory.getSlots()==0)return false;
                var stack=inventory.getStackInSlot(0);boolean changed=VisCharging.charge(stack,payment::pay);
                var source=payment.source();var data=source.getPersistentData();var credit=data.getCompound("ThaumicOmniverseTabletRepair");
                changed|=VisItemRepair.repair(stack,source.getLevel(),null,credit,payment::pay);
                if(source.getLevel().getGameTime()%40==0){data.put("ThaumicOmniverseTabletRepair",credit);source.setChanged();}
                return changed;
            });
        });
    }
}
