package automagy.content;

import automagy.logic.FilterRules;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Ordered pairs: explicit references across both sheets precede either sheet's name rule. */
public final class EagerFilters {
    public record Sheet(ItemFilter filter, boolean blacklist) {}
    public static long limit(List<Sheet> sheets, ItemStack key, List<ItemStack> source, List<ItemStack> destination) {
        for (Sheet sheet:sheets) for(int slot=0;slot<ItemFilter.SIZE;slot++) if(sheet.filter.matches(sheet.filter.entry(slot),key)) {
            if(!sheet.filter.useCount)return sheet.blacklist?0:Long.MAX_VALUE;
            long count=InventoryTransfer.count(sheet.blacklist?source:destination,key,sheet.filter::matches);
            return Math.max(0,sheet.blacklist?count-sheet.filter.count(slot):sheet.filter.count(slot)-count);
        }
        for(Sheet sheet:sheets) if(!sheet.filter.name().isEmpty()&&FilterRules.nameMatches(sheet.filter.name(),key.getHoverName().getString()))return sheet.blacklist?0:Long.MAX_VALUE;
        return sheets.stream().anyMatch(s->!s.blacklist)?0:Long.MAX_VALUE;
    }
    public static boolean matches(List<Sheet> sheets,ItemStack key,ItemStack stack) {
        return sheets.isEmpty()?ItemStack.isSameItemSameTags(key,stack):sheets.stream().anyMatch(s->s.filter.matches(key,stack));
    }
    private EagerFilters() {}
}
