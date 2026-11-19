package heartwork.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import heartwork.Heartwork;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;

public class HeartworkLoot extends LootModifier {
    public static final Codec<HeartworkLoot> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, HeartworkLoot::new));
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTER = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Heartwork.ID);
    static { REGISTER.register("grass_seeds", () -> CODEC); }
    public HeartworkLoot(LootItemCondition[] conditions) { super(conditions); }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || !(state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN))) return generated;
        for (int i = 0; i < generated.size(); i++) {
            ItemStack stack = generated.get(i);
            if (!stack.is(Items.WHEAT_SEEDS)) continue;
            int roll = context.getRandom().nextInt(41);
            if (roll < 10) continue;
            String seed = roll < 18 ? "coffee_seeds" : roll < 26 ? "lettuce_seeds" : roll < 34 ? "strawberry_seeds" : "tea_seeds";
            generated.set(i, new ItemStack(Heartwork.item(seed), stack.getCount()));
        }
        return generated;
    }
}
