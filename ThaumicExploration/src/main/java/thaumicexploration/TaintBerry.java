package thaumicexploration;
import net.minecraft.world.item.*;
public class TaintBerry extends Item {
    // The original crop was never registered. Preserve the edible item without its null crop crash.
    public TaintBerry(){super(new Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationMod(.3F).build()));}
}
