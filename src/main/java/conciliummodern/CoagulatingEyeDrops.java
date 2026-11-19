package conciliummodern;

public final class CoagulatingEyeDrops extends net.minecraft.world.item.Item {
    public CoagulatingEyeDrops(){super(new Properties().durability(100).rarity(net.minecraft.world.item.Rarity.UNCOMMON));}
    @Override public int getUseDuration(net.minecraft.world.item.ItemStack stack){return 60;}
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(net.minecraft.world.item.ItemStack stack){return net.minecraft.world.item.UseAnim.BOW;}
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){player.startUsingItem(hand);return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public net.minecraft.world.item.ItemStack finishUsingItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.LivingEntity user){
        if(user instanceof net.minecraft.server.level.ServerPlayer player && Lithography.inscribed(player)){
            Lithography.toggle(player);stack.hurtAndBreak(1,player,entity->entity.broadcastBreakEvent(entity.getUsedItemHand()));
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.PLAYERS,1,1+level.random.nextFloat()*.1f);
        }return stack;
    }
}
