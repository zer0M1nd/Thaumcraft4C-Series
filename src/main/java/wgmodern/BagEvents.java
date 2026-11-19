package wgmodern;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import tc4revival.content.CastingWandItem;
public class BagEvents {
    private static void primal(Aspect aspect,int amount,Map<Aspect,Integer> result){var parts=aspect.getComponents();if(parts.length==0)result.merge(aspect,amount,Math::addExact);else for(var part:parts)primal(part,amount,result);}
    @SubscribeEvent public static void pickup(EntityItemPickupEvent e){if(e.getEntity().level().isClientSide||e.getItem().isRemoved())return;var p=e.getEntity();var stack=e.getItem().getItem();for(int i=0;i<p.getInventory().getContainerSize();i++){var candidate=p.getInventory().getItem(i);if(!(candidate.getItem() instanceof BagItem bag))continue;var slots=BagItem.contents(candidate);
        if(bag.kind.equals("void")){boolean match=slots.stream().anyMatch(f->!f.isEmpty()&&ItemStack.isSameItemSameTags(f,stack));if(!match)continue;var amounts=new AspectList(stack);var primals=new LinkedHashMap<Aspect,Integer>();for(var aspect:amounts.getAspects())primal(aspect,amounts.getAmount(aspect),primals);if(!primals.isEmpty()){var entries=new ArrayList<>(primals.entrySet());var chosen=entries.get(p.getRandom().nextInt(entries.size()));for(int hotbar=0;hotbar<9;hotbar++){var wandStack=p.getInventory().getItem(hotbar);if(wandStack.getItem() instanceof CastingWandItem wand&&wand.vis(wandStack,chosen.getKey().unwrap())<wand.capacity(wandStack)){wand.addVis(wandStack,chosen.getKey().unwrap(),chosen.getValue(),true,true);break;}}}e.getItem().discard();e.setCanceled(true);p.getInventory().setChanged();return;}
        if(!bag.kind.equals("hungry")||!BagItem.permitted(candidate,p)||!stack.getItem().canFitInsideContainerItems())continue;
        for(int slot=0;slot<18&&!stack.isEmpty();slot++){var current=slots.get(slot);if(current.isEmpty()){int count=Math.min(stack.getCount(),stack.getMaxStackSize());slots.set(slot,stack.split(count));}else if(ItemStack.isSameItemSameTags(current,stack)){int count=Math.min(stack.getCount(),current.getMaxStackSize()-current.getCount());current.grow(count);stack.shrink(count);}}
        BagItem.store(candidate,slots);if(p.containerMenu instanceof BagMenu menu)menu.refreshPickup(candidate);p.getInventory().setChanged();if(stack.isEmpty()){e.getItem().discard();e.setCanceled(true);return;}
    }}
}
