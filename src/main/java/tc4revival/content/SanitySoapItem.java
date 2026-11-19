package tc4revival.content;

import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.network.*;

public  class SanitySoapItem extends Item {
    public SanitySoapItem() { super(new Properties()); }
    @Override public int getUseDuration(ItemStack stack) { return 200; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BRUSH; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        player.startUsingItem(hand); return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) {
        if (!(entity instanceof Player player)) return;
        if (200-remaining>195) player.releaseUsingItem();
        if (level.isClientSide) {
            if (level.random.nextFloat()<.2f) level.playLocalSound(player.getX(),player.getY(),player.getZ(),TCSounds.ROOTS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.1f,1.5f+level.random.nextFloat()*.2f,false);
            tc4revival.client.CrucibleEffects.soap(player,false);
        }
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) {
        if (!(entity instanceof Player player) || 200-remaining<=195) return;
        if(!player.getAbilities().instabuild)stack.shrink(1);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean reduced=KnowledgeSavedData.get(serverPlayer.server).update(player.getUUID(),level.random::nextInt,knowledge->
                    WarpCleansing.wash(knowledge,player.hasEffect(TCEffects.WARP_WARD.get()),level.getBlockState(player.blockPosition()).is(TCCleansing.PURE_WATER.get()),level.random.nextFloat()));
            if (reduced) TCNetwork.send(serverPlayer,new FocusEffectPacket(level.dimension().location(),FocusEffectPacket.Kind.WARP_RELIEF,player.position(),player.position(),1,0));
        } else if (level.isClientSide) {
            level.playLocalSound(player.getX(),player.getY(),player.getZ(),TCSounds.CRAFT_START.get(),net.minecraft.sounds.SoundSource.PLAYERS,.25f,1,false);
            tc4revival.client.CrucibleEffects.soap(player,true);
        }
    }
}
