package automagy.content;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import automagy.logic.FilterRules;

/** Explicit item entries take precedence over name rules across both filters. */
public final class HandMirrorFilters {
    public record Rule(ItemFilter filter,boolean blacklist) {}
    public static int maximum(List<Rule> rules,ItemStack item,List<ItemStack> inventory,int incoming){
        if(item.isEmpty()||incoming<=0)return 0;
        for(var rule:rules)for(int slot=0;slot<ItemFilter.SIZE;slot++){
            var filter=rule.filter();var reference=filter.entry(slot);if(reference.isEmpty()||!filter.matches(reference,item))continue;
            if(!filter.useCount)return rule.blacklist()?0:incoming;
            if(!rule.blacklist())return Math.min(incoming,filter.count(slot));
            long available=incoming;
            for(var held:inventory)if(filter.matches(reference,held))available+=held.getCount();
            return (int)Math.max(0,Math.min(incoming,available-filter.count(slot)));
        }
        for(var rule:rules)if(!rule.filter().name().isEmpty()&&FilterRules.nameMatches(rule.filter().name(),item.getHoverName().getString()))return rule.blacklist()?0:incoming;
        return 0;
    }
    private HandMirrorFilters(){}
}
