package dyesmodern;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThaumicDyes.ID)
public final class ThaumicDyes {
    public static final String ID="thaumicdyes";
    public ThaumicDyes() {
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        DyesItems.ITEMS.register(bus);DyesItems.TABS.register(bus);
        DyesRecipe.SERIALIZERS.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
            thaumcraft.api.equipment.BootsBehaviorProviders.registerMovementProvider(ID+":traveller",0,
                stack->stack.getItem() instanceof DyesArmorItem item&&item.traveller(),
                (context,current)->!context.flying()&&context.forward()&&!context.grounded()?(context.hovering()?.03f:.05f):current)));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(DyesProtection::setup));
        DyesNetwork.register();
    }
}
