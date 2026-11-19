package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import ic2.api.item.ElectricItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="emt")
public class ElectricCloudCreation {
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event){
        var level=event.getLevel();if(level.isClientSide||event.getItemStack().isEmpty()||!level.getBlockState(event.getPos()).is(tc4revival.content.TCArcane.NITOR.get()))return;
        // Original transfer limit, tier and external-discharge rules are deliberate.
        // An undercharged battery loses its partial discharge without converting the flame.
        double extracted=ElectricItem.manager.discharge(event.getItemStack(),256,4,false,true,false);
        if(extracted>=256)level.setBlockAndUpdate(event.getPos(),ElectroMagicTools.CLOUD.get().defaultBlockState());
    }
}
