package emtmodern.item;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/** Original EMTEventHandler.onSetEntityAttack rejects all mob targets wearing the Ring. */
@Mod.EventBusSubscriber(modid="emt")
public class OneRingProtection {
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){
        if(event.getEntity() instanceof Mob&&event.getNewTarget() instanceof Player player&&CuriosApi.getCuriosInventory(player).resolve().map(handler->handler.findFirstCurio(stack->stack.getItem() instanceof OneRing).isPresent()).orElse(false))event.setNewTarget(null);
    }
}
