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
    private final AspectAmounts stored=new AspectAmounts();
    public void tick(ServerLevel level,BlockPos pos,ItemStack pick,ServerPlayer worker,Runnable changed) {
        if (pick.isEmpty() || !(pick.getItem() instanceof net.minecraft.world.item.PickaxeItem)) return;
        if (counter++%40==0 && pick.isDamaged()) repair(level,pos,pick,worker,changed);
        worker.tickCount=(int)counter;
        // The original calls the installed pick's inventory update even when the bore is off.
        pick.inventoryTick(level,worker,0,true);
    }
    private void repair(ServerLevel level,BlockPos pos,ItemStack pick,ServerPlayer worker,Runnable changed) {
        int rank=pick.getEnchantmentLevel(TCEnchantments.REPAIR.get());
        if (rank<=0) return;
        if (!pick.is(LegacyRepairEnchantment.REPAIRABLE)) return;
        var base=ItemAspects.baseSession().base(ForgeRegistries.ITEMS.getKey(pick.getItem()).toString()).orElse(null);
        if (base==null || base.isEmpty()) return;
        int restored=Math.min(rank,pick.getDamageValue());
        var cost=VisRepair.costs(TCAspects.catalog(),base,rank,restored);
        if (pick.getItem() instanceof RepairableItem repairable && !repairable.beforeVisRepair(pick,worker,rank)) return;
        for (var aspect:cost.aspects()) {
            int missing=cost.amount(aspect)-stored.amount(aspect);
            if (missing>0) stored.add(aspect,VisNetwork.drain(level,pos,aspect,missing));
        }
        for (var aspect:cost.aspects()) if (stored.amount(aspect)<cost.amount(aspect)) return;
        for (var aspect:cost.aspects()) stored.reduce(aspect,cost.amount(aspect));
        pick.setDamageValue(pick.getDamageValue()-restored); changed.run();
    }
}
