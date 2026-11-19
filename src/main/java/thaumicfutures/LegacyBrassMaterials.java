package thaumicfutures;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.*;
import com.gregtechceu.gtceu.api.data.chemical.material.info.*;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.minecraft.resources.ResourceLocation;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
public final class LegacyBrassMaterials {
    public static Material BRASS;
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){
        thaumicfutures.compat.FutureGregTechAddon.REGISTRATE.registerEventListeners(bus);
        bus.addListener((MaterialRegistryEvent e)->{GTCEuAPI.materialManager.createRegistry("thaumicfutures");});
        bus.addListener((MaterialEvent e)->{
            BRASS=new Material.Builder(ResourceLocation.parse("thaumicfutures:alchemical_brass")).langValue("Alchemical Brass").ingot().color(0xC49A51).iconSet(MaterialIconSet.METALLIC)
                .flags(GENERATE_PLATE,GENERATE_ROD,GENERATE_BOLT_SCREW,GENERATE_GEAR,DISABLE_DECOMPOSITION).buildAndRegister();
            TagPrefix.ingot.setIgnored(BRASS,LegacyContent.BRASS::get);
            TagPrefix.nugget.setIgnored(BRASS,thaumicfutures.expansion.RestoredContent.BRASS_NUGGET::get);
            TagPrefix.block.setIgnored(BRASS,com.gregtechceu.gtceu.utils.memoization.GTMemoizer.memoizeBlockSupplier(thaumicfutures.expansion.RestoredContent.BRASS_BLOCK::get));
        });
    }
    public static void missing(net.minecraftforge.registries.MissingMappingsEvent event){
        for(var namespace:java.util.List.of("thaumicomniverse","gtceu"))for(var m:event.getMappings(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,namespace)){
            switch(m.getKey().getPath()){
                case "alchemical_brass_ingot"->m.remap(LegacyContent.BRASS.get());
                case "alchemical_brass_nugget"->m.remap(thaumicfutures.expansion.RestoredContent.BRASS_NUGGET.get());
                case "alchemical_brass_block"->m.remap(thaumicfutures.expansion.RestoredContent.BRASS_BLOCK.get().asItem());
            }
        }
    }
}
