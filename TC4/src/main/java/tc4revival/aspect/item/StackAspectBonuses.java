package tc4revival.aspect.item;

import java.util.List;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;

/** Ordered container/base/equipment/enchantment composition, without truncating distinct aspects. */
public  class StackAspectBonuses {
    public record EnchantmentContribution(String aspect, short level) {}
    private final AspectCatalog catalog;
    public StackAspectBonuses(AspectCatalog catalog) { this.catalog = catalog; }

    public AspectAmounts apply(AspectAmounts base, AspectAmounts contained, AspectAmounts equipment,
                               List<EnchantmentContribution> enchantments) {
        var result = new AspectAmounts();
        if (contained != null) contained.view().forEach((aspect, amount) -> { if (amount > 0) result.add(aspect, amount); });
        if (base != null) result.add(base);
        if (equipment != null) result.add(equipment);
        int levels = 0;
        for (var enchantment : enchantments) {
            if (enchantment.aspect() != null) result.merge(catalog.require(enchantment.aspect()), enchantment.level());
            levels += enchantment.level();
        }
        if (levels > 0) result.merge(catalog.require("praecantatio"), levels);
        return cull(result);
    }

    /** Compatibility entry point: retain every aspect, with no six-type truncation. */
    public AspectAmounts cull(AspectAmounts source) { return source.copy(); }
}
