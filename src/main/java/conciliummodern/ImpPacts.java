package conciliummodern;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.ilya3point999k.thaumicconcilium.api.event.WitcheryImpDealEvent;
/** Original 72000 online ticks of protection, preserved through saves and player cloning. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ImpPacts {
 private static final String KEY="thaumicconcilium:imp_deal_time";
 public static int remaining(Player p){return Math.max(0,p.getPersistentData().getInt(KEY));}
 public static void deal(Player p){if(!p.level().isClientSide)p.getPersistentData().putInt(KEY,72000);}
 @SubscribeEvent public static void deal(WitcheryImpDealEvent e){if(e.impOwner!=null)deal(e.impOwner);}
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&!e.player.level().isClientSide){int value=remaining(e.player);if(value>0)e.player.getPersistentData().putInt(KEY,value-1);}}
 @SubscribeEvent public static void clone(PlayerEvent.Clone e){e.getEntity().getPersistentData().putInt(KEY,remaining(e.getOriginal()));}
}
