package tc4revival.content;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;

public  class BoneBowItem extends BowItem implements RepairableItem {
    public BoneBowItem() { super(new Properties().durability(512)); }
    @Override public int getEnchantmentValue() { return 3; }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining) {
        if (getUseDuration(stack)-remaining>18) user.releaseUsingItem();
    }
    public static float charge(int ticks,boolean bone) {
        float amount=ticks/(bone?10f:20f); return Math.min(1,(amount*amount+amount*2)/3);
    }
    static boolean ordinaryAmmo(Player player) { return !player.getProjectile(player.getMainHandItem().getItem() instanceof BoneBowItem?player.getMainHandItem():player.getOffhandItem()).isEmpty(); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand); boolean ammo=!player.getProjectile(stack).isEmpty();
        var event=ForgeEventFactory.onArrowNock(stack,level,player,hand,ammo); if (event!=null) return event;
        if (!player.getAbilities().instabuild && !ammo && stack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS)==0) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand); return InteractionResultHolder.consume(stack);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining) {
        if (!(user instanceof Player player)) return;
        var ammo=player.getProjectile(stack);
        boolean canCreate=player.getAbilities().instabuild || stack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS)>0;
        int ticks=ForgeEventFactory.onArrowLoose(stack,level,player,getUseDuration(stack)-remaining,canCreate||!ammo.isEmpty());
        if(ticks<0 || ammo.isEmpty() && !canCreate)return;
        if(ammo.isEmpty())ammo=new ItemStack(Items.ARROW);
        boolean unlimited=player.getAbilities().instabuild || ammo.getItem() instanceof ArrowItem item && item.isInfinite(ammo,stack,player);
        float power=charge(ticks,true); if (power<.1f || level.isClientSide) return;
        var arrowItem=ammo.getItem() instanceof ArrowItem item?item:(ArrowItem)Items.ARROW;
        var arrow=customArrow(arrowItem.createArrow(level,ammo,player));
        arrow.shootFromRotation(player,player.getXRot(),player.getYRot(),0,power*3.75f,1);
        arrow.setBaseDamage(arrow.getBaseDamage()+.5);
        PrimalBowEvents.enchant(arrow,stack,0);
        if (unlimited) arrow.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        if (!level.addFreshEntity(arrow)) return;
        PrimalBowEvents.wearAndSound(player,stack,power);
        if (!unlimited) {ammo.shrink(1);if(ammo.isEmpty())player.getInventory().removeItem(ammo);}
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
    }
}
