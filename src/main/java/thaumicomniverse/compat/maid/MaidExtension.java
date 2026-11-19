package thaumicomniverse.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.*;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

/** Discovered only by Touhou Little Maid, keeping the host dependency optional. */
@LittleMaidExtension
public final class MaidExtension implements ILittleMaid {
    private static boolean registered;
    public MaidExtension() { if(!registered){registered=true;net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(MaidEquipmentEvents.class);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(MaidCharging.class);} }
    @Override public void addMaidTask(TaskManager manager) { manager.add(new MaidWandTask()); }
    @Override public void bindMaidBauble(com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager manager) {
        for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS)
            if(item instanceof top.theillusivec4.curios.api.type.capability.ICurioItem && (item instanceof tc4revival.content.RunicGear || item instanceof tc4revival.content.VisDiscountGear || net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).getNamespace().equals("thaumcraft")))
                manager.bind(item,new com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble() {});
    }
}
