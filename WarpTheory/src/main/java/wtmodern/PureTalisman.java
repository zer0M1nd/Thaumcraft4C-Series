package wtmodern;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;
public class PureTalisman extends WarpItem implements ICurioItem {
    public PureTalisman(){super("amulet",1);}
    @Override public void curioTick(SlotContext context,ItemStack stack){if(context.entity() instanceof ServerPlayer p&&p.tickCount>0&&p.tickCount%500==0){int warp=WarpState.total(p);if(warp>0&&p.getRandom().nextInt(100)<=Math.sqrt(warp)){var e=WarpState.queueOne(p,warp);WarpState.remove(p,e==null?1:e.cost());}}}
}
