package tc4revival.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.sounds.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public  class PrimalBowEvents {
    static void position(AbstractArrow arrow,net.minecraft.world.entity.LivingEntity owner,boolean ahead) {
        float yaw=owner.getYRot()*(float)Math.PI/180;
        arrow.setOwner(owner); arrow.setYRot(owner.getYRot()); arrow.setXRot(owner.getXRot());
        arrow.setPos(owner.getX()-net.minecraft.util.Mth.cos(yaw)*.16f,owner.getEyeY()-.10000000014901161,owner.getZ()-net.minecraft.util.Mth.sin(yaw)*.16f);
        if (ahead) arrow.setPos(arrow.position().add(owner.getLookAngle()));
    }
    static ItemStack ammunition(Player player) {
        return player.getInventory().items.stream().filter(stack->!stack.isEmpty() && stack.getItem() instanceof PrimalArrowItem).findFirst().orElse(ItemStack.EMPTY);
    }
    static void enchant(AbstractArrow arrow,ItemStack bow,int extraPunch) {
        int power=bow.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        if (power>0) arrow.setBaseDamage(arrow.getBaseDamage()+power*.5+.5);
        int punch=bow.getEnchantmentLevel(Enchantments.PUNCH_ARROWS)+extraPunch;
        if (punch>0) arrow.setKnockback(punch);
        if (bow.getEnchantmentLevel(Enchantments.FLAMING_ARROWS)>0) arrow.setSecondsOnFire(100);
    }
    static void wearAndSound(Player player,ItemStack bow,float charge) {
        bow.hurtAndBreak(1,player,entity->entity.broadcastBreakEvent(player.getUsedItemHand()));
        player.level().playSound(null,player,SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1/(player.getRandom().nextFloat()*.4f+1.2f)+charge*.5f);
    }
    private PrimalBowEvents() {}
}
