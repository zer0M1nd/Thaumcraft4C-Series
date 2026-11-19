package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.TCAspects;

/** Harness fuel lives in a real, removable jar. Charge belongs to the harness, not the jar. */
public  class HoverFuel {
    private HoverFuel() {}
    public static ItemStack jar(ItemStack harness) {
        return harness.hasTag() ? ItemStack.of(harness.getTag().getCompound("jar")) : ItemStack.EMPTY;
    }
    public static void setJar(ItemStack harness, ItemStack jar) {
        harness.getOrCreateTag().put("jar", jar.isEmpty() ? new CompoundTag() : jar.save(new CompoundTag()));
    }
    public static int jarFuel(ItemStack jar) {
        if (!(jar.getItem() instanceof EssentiaJarItem container)) return 0;
        return (short) container.storedAspects(jar).amount(TCAspects.get(LegacyAspect.ENERGY));
    }
    public static int fuel(ItemStack harness) { return jarFuel(jar(harness)); }
    public static boolean accepts(ItemStack jar) { return jarFuel(jar) > 0; }
    public static float inertia(int haste, boolean girdle) { return Math.min(1, .7f + .075f * haste + (girdle ? .21f : 0)); }
    public static boolean expend(ItemStack harness, boolean girdle) {
        if (!harness.hasTag() || !harness.getTag().contains("jar")) return false;
        var tag = harness.getTag();
        if (!tag.contains("charge")) tag.putShort("charge", (short) 0);
        var jar = jar(harness);
        int fuel = jarFuel(jar);
        if (fuel <= 0) return false;
        short charge = tag.getShort("charge");
        // The old girdle really shortens the interval; do not invert its 0.8 modifier.
        if (charge < 360f * (girdle ? .8f : 1)) {
            tag.putShort("charge", (short) (charge + 1));
            return true;
        }
        tag.putShort("charge", (short) 0);
        var remaining = new AspectAmounts();
        if (--fuel > 0) remaining.add(TCAspects.get(LegacyAspect.ENERGY), fuel);
        ((EssentiaJarItem) jar.getItem()).setStoredAspects(jar, remaining);
        setJar(harness, jar);
        return fuel > 0;
    }
}
