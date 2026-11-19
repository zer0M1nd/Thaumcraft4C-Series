package thaumicomniverse.compat.create;

import java.util.Optional;
import tc4revival.content.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import thaumcraft.api.devices.DeviceExtensions;

public final class CreateDevices {
    public static final DeviceExtensions.Key<CrucibleBridge> CRUCIBLE=new DeviceExtensions.Key<>("thaumicomniverse:crucible",CrucibleBridge.class);
    public static final DeviceExtensions.Key<PedestalBridge> PEDESTAL=new DeviceExtensions.Key<>("thaumicomniverse:pedestal",PedestalBridge.class);
    public static final DeviceExtensions.Key<ChargerBridge> CHARGER=new DeviceExtensions.Key<>("thaumicomniverse:charger",ChargerBridge.class);
    public static final DeviceExtensions.Key<MirrorBridge> MIRROR=new DeviceExtensions.Key<>("thaumicomniverse:mirror",MirrorBridge.class);
    private CreateDevices() {}
    public static void setup(){
        DeviceExtensions.register(CRUCIBLE,0,CrucibleBlockEntity.class,1,"OmniverseBelt",CrucibleBridge::new);
        DeviceExtensions.register(PEDESTAL,0,ArcanePedestalBlockEntity.class,1,"OmniverseCreate",PedestalBridge::new);
        DeviceExtensions.register(CHARGER,0,WorkbenchChargerBlockEntity.class,1,"",ChargerBridge::new);
        DeviceExtensions.register(MIRROR,0,ItemMirrorBlockEntity.class,1,"",MirrorBridge::new);
        thaumcraft.api.transport.MirrorOutputRoutes.register("thaumicomniverse:create",0,(mirror,offered)->{
            var count=MirrorBridge.output(mirror,offered);return count==null?java.util.OptionalInt.empty():java.util.OptionalInt.of(count);
        });
        thaumcraft.api.crafting.InfusionLifecycle.observeCompleted("thaumicomniverse:create",0,context->
            DeviceExtensions.component(context.center(),PEDESTAL).ifPresent(PedestalBridge::infusionFinished));
    }
    public static Optional<SmartBlockEntity> bridge(BlockEntity entity){
        if(entity instanceof CrucibleBlockEntity)return DeviceExtensions.component(entity,CRUCIBLE).map(value->value);
        if(entity instanceof ArcanePedestalBlockEntity)return DeviceExtensions.component(entity,PEDESTAL).map(value->value);
        if(entity instanceof WorkbenchChargerBlockEntity)return DeviceExtensions.component(entity,CHARGER).map(value->value);
        if(entity instanceof ItemMirrorBlockEntity)return DeviceExtensions.component(entity,MIRROR).map(value->value);
        return Optional.empty();
    }
}
