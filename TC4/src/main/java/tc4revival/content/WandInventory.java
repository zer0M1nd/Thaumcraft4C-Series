package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import top.theillusivec4.curios.api.CuriosApi;

public  class WandInventory {
    private WandInventory() {}
    public static List<ItemStack> wornBaubles(Player player) {
        var result = new ArrayList<ItemStack>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            // TC4 Baubles order: amulet, two rings, belt. Cosmetic slots do not apply.
            for (String name : List.of("necklace", "ring", "belt")) inventory.getStacksHandler(name).ifPresent(handler -> {
                var stacks = handler.getStacks(); int limit = name.equals("ring") ? 2 : 1;
                for (int i = 0; i < Math.min(limit, stacks.getSlots()); i++) result.add(stacks.getStackInSlot(i));
            });
        });
        return result;
    }
    public static float discount(Player player, Aspect aspect) {
        if (player == null) return 0;
        double cost=player.getAttributeValue(TCAttributes.VIS_COST.get());
        var specific=aspect==null?null:TCAttributes.ASPECT_COSTS.get(aspect.tag());
        if(specific!=null)cost+=player.getAttributeValue(specific.get());
        return (float)(1-cost);
    }
    public interface VisAmulet {
        boolean consumeVis(ItemStack stack, Player player, Map<Aspect, Integer> costs, boolean commit);
    }
    public static boolean consume(Player player, Map<Aspect, Integer> costs) {
        if (player.level().isClientSide) return false;
        for (var stack : wornBaubles(player)) if (stack.getItem() instanceof VisAmulet amulet && amulet.consumeVis(stack, player, costs, true)) return true;
        var items = player.getInventory().items;
        for (int index = items.size() - 1; index >= 0; index--) {
            var stack = items.get(index);
            if (stack.getItem() instanceof CastingWandItem wand && wand.consumeForPlayer(stack, player, costs, false, true, true)) return true;
        }
        return false;
    }
}
