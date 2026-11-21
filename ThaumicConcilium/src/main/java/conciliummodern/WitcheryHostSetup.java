package conciliummodern;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** No optional types in this discovered class: the adapter loads only with the supported host. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class WitcheryHostSetup {
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){
        if(!ModList.get().isLoaded("witchery"))return;
        event.enqueueWork(()->{
            try{Class.forName("com.emoniph.witchery.init.WitcheryItems",false,WitcheryHostSetup.class.getClassLoader());}
            catch(ClassNotFoundException absent){com.mojang.logging.LogUtils.getLogger().warn("Unsupported Witchery host: DoomCat 1.20.1 API is absent; Concilium integration stays disabled");return;}
            DoomCatWitcheryHost.install();
        });
    }
    private WitcheryHostSetup(){}
}
