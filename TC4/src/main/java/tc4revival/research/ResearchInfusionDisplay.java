package tc4revival.research;

import java.util.Optional;
import tc4revival.research.ResearchPage.*;

/** Resolves infusion display descriptions before legacy item identities are migrated. */
public  class ResearchInfusionDisplay {
    private ResearchInfusionDisplay() {}

    public static ResearchPageRecipes.Infusion definition(Recipe recipe, ResearchPageRecipes recipes) {
        if (recipe instanceof Reference reference
                && recipes.require(reference.keys().get(0)).definition() instanceof ResearchPageRecipes.Infusion infusion)
            return infusion;
        if (recipe instanceof RunicExample example)
            return new ResearchPageRecipes.Infusion(example.research(), new Item(example.input()),
                    new Item(example.output()), example.ingredients(), example.aspects(), example.instability());
        throw new IllegalArgumentException("Missing infusion page definition");
    }

    public static Item output(ResearchPageRecipes.Infusion infusion) {
        if (infusion.output() instanceof Item item) return item;
        if (!(infusion.input() instanceof Item input)
                || !(infusion.output() instanceof Sequence change) || change.elements().size() != 2
                || !(change.elements().get(0) instanceof Literal key) || !(key.value() instanceof String name)
                || !(change.elements().get(1) instanceof Nbt value))
            throw new IllegalArgumentException("Invalid infusion property output");
        var stack = input.stack();
        var tag = stack.tag().orElseGet(net.minecraft.nbt.CompoundTag::new);
        // Parse the value in a fixed wrapper so arbitrary property names remain literal NBT keys.
        var property = LegacyStackNbt.read("{value:" + value.value() + "}").get("value");
        if (property == null) throw new IllegalArgumentException("Missing infusion property value");
        tag.put(name, property.copy());
        return new Item(new ResearchDefinition.Stack(stack.key(), stack.count(), stack.damageable(),
                Optional.of(tag.toString()), stack.ore()));
    }
}
