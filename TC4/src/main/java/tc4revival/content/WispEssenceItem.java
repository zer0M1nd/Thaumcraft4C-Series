package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

public  class WispEssenceItem extends Item implements StoredAspectItem {
    public WispEssenceItem() { super(new Properties()); }
    public ItemStack of(Aspect aspect) {
        var stack = new ItemStack(this); setStoredAspects(stack, new AspectAmounts().add(aspect, 2)); return stack;
    }
    public int tint(ItemStack stack) {
        var stored = storedAspects(stack);
        if (!stored.isEmpty()) return stored.aspects().get(0).color();
        var aspects = TCAspects.catalog().all();
        return aspects.isEmpty() ? 0xffffff : aspects.get(Math.floorMod(System.currentTimeMillis() / 500, aspects.size())).color();
    }
}
