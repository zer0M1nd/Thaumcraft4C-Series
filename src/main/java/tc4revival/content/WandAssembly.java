package tc4revival.content;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;

/** Shared matching core for data-driven arcane wand/sceptre recipe serializers. */
public  class WandAssembly {
    public record Ingredient(String item, int metadata, CompoundTag tag) {
        public Ingredient { tag = tag == null ? null : tag.copy(); }
        @Override public CompoundTag tag() { return tag == null ? null : tag.copy(); }
    }
    public record Result(WandParts.Cap cap, WandParts.Rod rod, boolean sceptre, int costPerPrimal, boolean unlocked) {
        public boolean hasOutput() { return sceptre || cap != WandParts.Cap.IRON || rod != WandParts.Rod.WOOD; }
        public CompoundTag components() {
            var tag = new CompoundTag(); tag.putString("cap", cap.tag); tag.putString("rod", rod.tag);
            if (sceptre) tag.putByte("sceptre", (byte) 1);
            return tag;
        }
    }
    private WandAssembly() {}
    /** Row-major 3x3 grid. Null denotes an empty cell; stack size is deliberately not a matching criterion. */
    public static Optional<Result> match(List<Ingredient> grid, boolean sceptre, Predicate<String> research) {
        if (grid.size() != 9) return Optional.empty();
        int[] occupied = sceptre ? new int[]{1, 2, 4, 5, 6} : new int[]{2, 4, 6};
        for (int i = 0; i < 9; i++) {
            boolean expected = false; for (int slot : occupied) expected |= slot == i;
            if (expected != (grid.get(i) != null)) return Optional.empty();
        }
        Ingredient first = grid.get(sceptre ? 1 : 6);
        if (!equal(first, grid.get(sceptre ? 5 : 2), sceptre)) return Optional.empty();
        if (sceptre && (!equal(first, grid.get(6), true)
                || !equal(grid.get(2), new Ingredient("Thaumcraft:ItemResource", 15, null), true))) return Optional.empty();
        WandParts.Cap cap = null; WandParts.Rod rod = null;
        for (var candidate : WandParts.Cap.values())
            if (equal(first, candidate.item==null?new Ingredient("Thaumcraft:WandCap", candidate.metadata, null):new Ingredient(candidate.item,0,null), sceptre)) { cap = candidate; break; }
        for (var candidate : WandParts.Rod.values()) {
            var expected = candidate.item!=null?new Ingredient(candidate.item,0,null):candidate == WandParts.Rod.WOOD ? new Ingredient("minecraft:stick", 0, null)
                    : new Ingredient("Thaumcraft:WandRod", candidate.metadata, null);
            if (equal(grid.get(4), expected, sceptre)) { rod = candidate; break; }
        }
        if (cap == null || rod == null) return Optional.empty();
        int cost = cap.craftCost * rod.craftCost;
        if (sceptre) cost = (int) (cost * 1.5f);
        return Optional.of(new Result(cap, rod, sceptre, cost,
                (cap.research().isEmpty()||research.test(cap.research())) && (rod.research().isEmpty()||research.test(rod.research())) && (!sceptre || research.test("SCEPTRE"))));
    }
    private static boolean equal(Ingredient target, Ingredient input, boolean sceptre) {
        if (target == null || input == null) return target == input;
        if (!target.item.equals(input.item)) return false;
        if (sceptre) return target.metadata == input.metadata;
        return (target.tag == null || target.tag.equals(input.tag)) && (target.metadata == 32767 || target.metadata == input.metadata);
    }
}
