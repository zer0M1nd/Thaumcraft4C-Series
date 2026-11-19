package com.ilya3point999k.thaumicconcilium.api.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/** Hook for a modern Witchery adapter after it commits a Flame Imp transaction. */
public class WitcheryImpDealEvent extends Event {
    public final Player impOwner;
    public final ItemStack stack;
    public final LivingEntity target;
    public WitcheryImpDealEvent(Player owner,ItemStack stack,LivingEntity target){impOwner=owner;this.stack=stack.copy();this.target=target;}
    public static void fireImpDealEvent(Player owner,ItemStack stack,LivingEntity target){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new WitcheryImpDealEvent(owner,stack,target));}
}
