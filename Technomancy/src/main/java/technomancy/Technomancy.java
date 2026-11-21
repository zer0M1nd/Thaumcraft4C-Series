package technomancy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Technomancy.ID)
public final class Technomancy {
    public static final String ID="technomancy";
    public Technomancy(){
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        TechRegistry.register(bus);
        TechFluids.register(bus);
        TechNetwork.register();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(()->{
            TechWands.setup();
            for(String mod:new String[]{"botania","bloodmagic"})
                thaumcraft.api.research.ResearchCategories.registerEnablement("technomancy:"+mod.toUpperCase(java.util.Locale.ROOT),()->net.minecraftforge.fml.ModList.get().isLoaded(mod));
        }));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,TechConfig.SPEC);
    }
    public static ResourceLocation id(String path){return new ResourceLocation(ID,path);}
}
