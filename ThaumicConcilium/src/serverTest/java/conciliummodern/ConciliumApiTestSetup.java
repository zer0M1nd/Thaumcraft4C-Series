package conciliummodern;

import com.ilya3point999k.thaumicconcilium.api.ThaumicConciliumApi;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.aspects.*;

/** Only the serverTest source set installs these recipes, before the server's real initial reload. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConciliumApiTestSetup {
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(()->{
        var output=new ItemStack(Items.ECHO_SHARD,2);output.getOrCreateTag().putString("concilium_test","output");
        ThaumicConciliumApi.addChainedRecipe("thaumicconcilium:validation/api_reload","",output,new ItemStack(Items.EMERALD),new AspectList().add(Aspect.AIR,17));
        ThaumicConciliumApi.addPolishmentRecipe(new ItemStack(Items.DIORITE),new AspectList().add(Aspect.EARTH,31));
    });}
    private ConciliumApiTestSetup(){}
}
