package heartwork.compat.blade;

import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

public final class BotaniaBladeCompat {
    public static void tickStand(SlashBladeEvent.BladeStandTickEvent event) {
        var stand=event.getBladeStand();var blade=event.getBlade();
        if(stand.level().isClientSide||!blade.isDamaged()||!"item.heartwork.yingjian".equals(event.getSlashBladeState().getTranslationKey()))return;
        if(stand.level().getBlockEntity(stand.blockPosition().below()) instanceof ManaPoolBlockEntity pool&&pool.getCurrentMana()>=75){
            pool.receiveMana(-75);blade.setDamageValue(Math.max(0,blade.getDamageValue()-3));pool.setChanged();
            // Item-frame stack data must be dirtied for tracking clients as well.
            stand.setItem(blade.copy());
        }
    }
}
