package automagy.content;

import automagy.Automagy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

@Mod.EventBusSubscriber(modid=Automagy.ID)
public final class AutoHandMirrorEvents {
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event){
        var player=event.getEntity();var entity=event.getItem();
        if(player.level().isClientSide||!entity.isAlive()||entity.hasPickUpDelay()||player.isSpectator())return;
        if(player.getInventory().items.stream().noneMatch(s->s.getItem() instanceof AutoHandMirrorItem))return;
        // getOwner() identifies the thrower entity, not vanilla's pickup target UUID.
        // The event fires before the target restriction in ItemEntity.playerTouch.
        var saved=new net.minecraft.nbt.CompoundTag();entity.addAdditionalSaveData(saved);
        if(saved.hasUUID("Owner")&&!saved.getUUID("Owner").equals(player.getUUID()))return;
        var original=entity.getItem();int left=original.getCount();if(left<=0)return;
        // Legacy pickup interception scans main inventory only, including the hotbar.
        for(int slot=0;slot<36&&left>0;slot++){
            var mirror=player.getInventory().getItem(slot);if(!(mirror.getItem() instanceof AutoHandMirrorItem))continue;
            var held=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            for(int i=0;i<player.getInventory().getContainerSize();i++)held.add(player.getInventory().getItem(i));
            int amount=HandMirrorFilters.maximum(AutoHandMirrorItem.rules(mirror),original,held,left);
            if(amount>0&&AutoHandMirrorItem.transport(mirror,original.copyWithCount(amount),player))left-=amount;
        }
        if(left==original.getCount())return;
        player.take(entity,original.getCount()-left);
        // Vanilla retains this exact stack reference across the Forge event. Replacing it would
        // let vanilla add the original full stack after we already sent part through the mirror.
        original.setCount(left);
        if(left==0){entity.discard();event.setCanceled(true);}else entity.setItem(original);
    }
    private AutoHandMirrorEvents(){}
}
