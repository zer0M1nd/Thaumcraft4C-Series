package thaumicfutures.compat.bees;

import forestry.api.ForestryTags;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.apiculture.items.ItemHiveFrame;
import forestry.apiculture.items.ItemScoop;
import forestry.arboriculture.items.ItemGrafter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import tc4revival.content.ThaumiumToolTier;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ThaumicBeeTools {
    private ThaumicBeeTools() {}

    public static void register(DeferredRegister<Item> items) {
        items.register("thaumium_scoop", Scoop::new);
        items.register("thaumium_grafter", Grafter::new);
        items.register("bee_frame_magic", MagicFrame::new);
        items.register("bee_frame_resilient", () -> new MagicFrame(800, 1, 1, 2, .5f));
        items.register("bee_frame_gentle", () -> new MagicFrame(200, .7f, 1.5f, 1.4f, .01f));
        items.register("bee_frame_metabolic", () -> new MagicFrame(130, 1.8f, 1, 1.2f, 1));
        items.register("bee_frame_necrotic", () -> new MagicFrame(280, 1, .3f, .75f, 1.2f));
        items.register("bee_frame_temporal", () -> new MagicFrame(300, 1, 2.5f, 1, .8f));
        items.register("bee_frame_oblivion", () -> new MagicFrame(50, 1, .0001f, 0, 1));
    }

    public static final class Scoop extends ItemScoop implements thaumcraft.api.IRepairable {
        public Scoop() { super(30); }
        @Override public int getEnchantmentValue() { return ThaumiumToolTier.INSTANCE.getEnchantmentValue(); }
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
            return ThaumiumToolTier.INSTANCE.getRepairIngredient().test(material);
        }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
            return state.is(ForestryTags.Blocks.MINEABLE_SCOOP) ? 4.8f : 1;
        }
    }

    public static final class Grafter extends ItemGrafter implements thaumcraft.api.IRepairableExtended {
        public Grafter() { super(15); }
        @Override public int getEnchantmentValue() { return ThaumiumToolTier.INSTANCE.getEnchantmentValue(); }
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
            return ThaumiumToolTier.INSTANCE.getRepairIngredient().test(material);
        }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
            return state.is(ForestryTags.Blocks.MINEABLE_GRAFTER) ? 4.8f : 1;
        }
        @Override public boolean doRepair(ItemStack stack, Player player, int level) {
            if (!stack.isDamaged()) return false;
            player.causeFoodExhaustion(.6f * level * level);
            return true;
        }
    }

    public static final class MagicFrame extends ItemHiveFrame {
        private static final IBeeModifier INACTIVE = new IBeeModifier() {};
        private final IBeeModifier modifier;
        private final float mutation, lifespan, production, decay;
        public MagicFrame() { this(240, 1, 1, 2, .6f); }
        public MagicFrame(int durability, float mutation, float lifespan, float production, float decay) {
            super(durability, decay);
            this.mutation = mutation;
            this.lifespan = lifespan;
            this.production = production;
            this.decay = decay;
            modifier = new IBeeModifier() {
                @Override public float modifyProductionSpeed(IGenome genome, float current) { return current * production; }
                @Override public float modifyGeneticDecay(IGenome genome, float current) { return current * decay; }
                @Override public float modifyAging(IGenome genome, IGenome mate, float current) { return current / lifespan; }
                @Override public float modifyMutationChance(IGenome genome, IGenome mate,
                        IMutation<IBeeSpecies> recipe, float current) { return current * mutation; }
            };
        }
        @Override public IBeeModifier getBeeModifier(ItemStack frame) { return thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled() ? modifier : INACTIVE; }
        @Override public int getMaxStackSize(ItemStack stack) { return 1; }
        @Override public void appendHoverText(ItemStack stack, Level world, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("omniverse.bees.frame_modifiers", production, lifespan, mutation, decay));
            lines.add(Component.translatable("item.forestry.durability", stack.getMaxDamage() - stack.getDamageValue()));
        }
    }
}
