package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemAspects;

/** Machine repair owns a vis buffer; it does not borrow a player's wand inventory. */
public  class ArcaneBoreRepair {
    private long counter;
    private AspectAmounts cost=new AspectAmounts();
    private ItemStack previous=ItemStack.EMPTY;
    private final AspectAmounts stored=new AspectAmounts();
    public void tick(ServerLevel level,BlockPos pos,ItemStack pick,ServerPlayer worker,Runnable changed) {
        var identity=pick.copy(); if (identity.isDamageableItem()) identity.setDamageValue(0);
        if (!ItemStack.isSameItemSameTags(previous,identity)) {
            previous=identity; cost=new AspectAmounts();
        }
        if (pick.isEmpty() || !(pick.getItem() instanceof net.minecraft.world.item.PickaxeItem)) return;
        if (counter++%40==0 && pick.isDamaged()) repair(pick,worker,changed);
        if (!cost.isEmpty() && counter%5==0) {
            for (var aspect:cost.aspects()) if (stored.amount(aspect)<cost.amount(aspect))
                stored.add(aspect,VisNetwork.drain(level,pos,aspect,cost.amount(aspect)));
        }
        worker.tickCount=(int)counter;
        // The original calls the installed pick's inventory update even when the bore is off.
        pick.inventoryTick(level,worker,0,true);
    }
    private void repair(ItemStack pick,ServerPlayer worker,Runnable changed) {
        int rank=Math.min(2,pick.getEnchantmentLevel(TCEnchantments.REPAIR.get()));
        if (rank<=0) return;
        if (!(pick.getItem() instanceof RepairableItem repairable)) { cost=new AspectAmounts(); return; }
        var base=ItemAspects.baseSession().base(ForgeRegistries.ITEMS.getKey(pick.getItem()).toString()).orElse(null);
        if (base==null || base.isEmpty()) return;
        cost=VisRepair.costs(TCAspects.catalog(),base,rank);
        if (!repairable.beforeVisRepair(pick,worker,rank)) return;
        for (var aspect:cost.aspects()) if (stored.amount(aspect)<cost.amount(aspect)) return;
        for (var aspect:cost.aspects()) stored.reduce(aspect,cost.amount(aspect));
        pick.setDamageValue(Math.max(0,pick.getDamageValue()-rank)); changed.run();
    }
}
