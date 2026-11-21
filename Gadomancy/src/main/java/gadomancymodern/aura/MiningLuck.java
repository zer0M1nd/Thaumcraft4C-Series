package gadomancymodern.aura;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
public class MiningLuck {
    public static int bonus(ItemStack stack,Enchantment enchantment){if(stack.isEmpty()||enchantment!=Enchantments.BLOCK_FORTUNE&&enchantment!=Enchantments.MOB_LOOTING)return 0;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return 0;for(var player:server.getPlayerList().getPlayers())if(player.getMainHandItem()==stack){var effect=player.getEffect(GadEffects.LUCK.get());return effect==null?0:effect.getAmplifier()+1;}return 0;}
}
