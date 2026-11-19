package tc4revival.content;

import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public  class ElementalPickaxeItem extends PickaxeItem implements RepairableItem {
    public ElementalPickaxeItem() { super(ElementalToolTier.INSTANCE,1,-2.8f,new Properties().rarity(Rarity.RARE)); }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity target) {
        if (!player.level().isClientSide && (!(target instanceof Player) || player.getServer()!=null && player.getServer().isPvpAllowed())) target.setSecondsOnFire(2);
        return super.onLeftClickEntity(stack,player,target);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer(); if (player==null) return InteractionResult.PASS;
        context.getItemInHand().hurtAndBreak(5,player,owner->owner.broadcastBreakEvent(context.getHand()));
        var level=context.getLevel(); var pos=context.getClickedPos();
        if (!level.isClientSide) {
            level.playSound(null,pos,TCSounds.WAND_FAIL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,.2f+level.random.nextFloat()*.2f);
            if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)tc4revival.network.TCNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(()->serverPlayer),
                    new tc4revival.network.ElementalProspectingPacket(level.dimension().location(),pos,ElementalProspecting.scan(serverPlayer.serverLevel(),pos)));
        } else player.swing(context.getHand());
        return InteractionResult.PASS;
    }
}
